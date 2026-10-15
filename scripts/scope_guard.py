#!/usr/bin/env python3
"""Session-bound scope checks. Standard library only; Python >= 3.8."""

import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shlex
import stat
import subprocess
import sys
import tempfile
from typing import NamedTuple


class ScopeError(ValueError):
    """Invalid scope, approval, or workspace state."""


class Forbidden(ScopeError):
    """An action prohibited independently of task approval."""


class Decision(NamedTuple):
    mode: str
    reason: str


HISTORY_OPERATIONS = {"commit", "merge", "rebase", "pull"}
SENSITIVE = {"dependency", "migration", "policy", "push", "deploy"} | HISTORY_OPERATIONS
OPERATIONS = {"test", "build", "command"} | SENSITIVE
ARTIFACT_DIRS = {"target", "build", "build-output", "dist", "coverage",
                 "__pycache__", ".pytest_cache"}


def digest(path):
    checksum = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(65536), b""):
            checksum.update(chunk)
    return checksum.hexdigest()


def read_json(path):
    try:
        with path.open(encoding="utf-8") as stream:
            value = json.load(stream)
        if not isinstance(value, dict):
            raise ScopeError("Expected a JSON object")
        return value
    except (OSError, ValueError) as error:
        raise ScopeError("Cannot read valid JSON: " + str(path.name)) from error


def text_list(value, label, nonempty=False):
    if not isinstance(value, list) or (nonempty and not value):
        raise ScopeError(label + " must be a list" + (" with entries" if nonempty else ""))
    if any(not isinstance(item, str) or not item.strip() for item in value):
        raise ScopeError(label + " must contain nonempty strings")
    return value


def matches(path, patterns):
    return any(path == pattern or
               (pattern.endswith("/**") and path.startswith(pattern[:-3] + "/"))
               for pattern in patterns)


def command_tokens(command):
    # Shell syntax is intentionally unsupported, including substitutions inside
    # double quotes. Exact argv comparison alone does not make shell strings safe.
    if not isinstance(command, str) or re.search(r"[\n\r;&|<>`$]", command):
        raise ScopeError("Use one simple command; shell composition/substitution needs a different plan")
    try:
        tokens = shlex.split(command, comments=False, posix=True)
    except ValueError as error:
        raise ScopeError("Cannot parse command") from error
    if not tokens:
        raise ScopeError("Empty command")
    return tokens


def self_approval(tokens):
    return any(Path(item).name in {"scope_guard.py", "scope_guard"} for item in tokens) and "approve" in tokens


def git_operation(tokens):
    if Path(tokens[0]).name != "git" or len(tokens) < 2:
        return None
    args = tokens[1:]
    while args and args[0] in {"--no-pager", "--no-optional-locks"}:
        args = args[1:]
    if not args or args[0].startswith("-"):
        raise ScopeError("Unsupported Git global options; use the approved cwd instead of -C/-c")
    if args[0] in HISTORY_OPERATIONS | {"push"}:
        return args[0]
    if args[0] in ("add", "restore", "reset", "checkout", "switch", "clean",
                     "stash", "tag", "fetch", "config", "update-ref"):
        return "command"
    return None


class ScopeGuard:
    def __init__(self, root, session_id):
        self.root = Path(root).resolve()
        if not isinstance(session_id, str) or not re.fullmatch(r"[A-Za-z0-9_.:-]{1,200}", session_id):
            raise ScopeError("A valid runtime session_id is required")
        self.session_id = session_id
        if Path(self.git("rev-parse", "--show-toplevel").strip()).resolve() != self.root:
            raise ScopeError("--repo must identify the repository root")
        common = Path(self.git("rev-parse", "--git-common-dir").strip())
        self.common_dir = (self.root / common).resolve()
        name = hashlib.sha256(session_id.encode()).hexdigest() + ".json"
        self.receipt_path = self.common_dir / "mealio-scope" / name

    def git(self, *args):
        try:
            return subprocess.check_output(
                ["git", "--no-optional-locks", "-C", str(self.root), *args],
                stderr=subprocess.DEVNULL).decode("utf-8", "surrogateescape")
        except (OSError, subprocess.CalledProcessError) as error:
            raise ScopeError("Git inspection failed") from error

    def path(self, raw, cwd=None, allow_root=False):
        if not isinstance(raw, str) or not raw or "\x00" in raw or "\\" in raw:
            raise Forbidden("Invalid path")
        if ".." in PurePosixPath(raw).parts:
            raise Forbidden("Path traversal is prohibited")
        target = Path(raw)
        if not target.is_absolute():
            target = Path(cwd or self.root) / target
        try:
            relative = target.relative_to(self.root)
        except ValueError as error:
            raise Forbidden("Path is outside the repository") from error
        if ".git" in relative.parts:
            raise Forbidden("Direct Git-internal writes are prohibited")
        cursor = self.root
        for part in relative.parts:
            cursor = cursor / part
            if cursor.is_symlink():
                raise Forbidden("Symlink paths require manual handling")
        if not relative.parts and not allow_root:
            raise Forbidden("Repository-wide path permission is prohibited")
        return relative.as_posix()

    def pattern(self, value):
        if not isinstance(value, str) or value.startswith("/"):
            raise ScopeError("Scope paths must be repository-relative")
        base = value[:-3] if value.endswith("/**") else value
        if any(char in base for char in "*?["):
            raise ScopeError("Only exact files or a narrow directory/** are supported")
        return self.path(base) + ("/**" if value.endswith("/**") else "")

    def validate_scope(self, scope):
        version = scope.get("version")
        if version not in (1, 2):
            raise ScopeError("Unsupported scope version")
        for label in ("task_id", "spec", "plan"):
            if not isinstance(scope.get(label), str) or not scope[label].strip():
                raise ScopeError(label + " is required")
        if not re.fullmatch(r"[a-z0-9][a-z0-9-]{0,79}", scope["task_id"]):
            raise ScopeError("task_id must use lowercase letters, numbers, and hyphens")
        if version == 1:
            if not isinstance(scope.get("objective"), str) or not scope["objective"].strip():
                raise ScopeError("objective is required")
            for label in ("components", "acceptance_criteria", "exclusions"):
                text_list(scope.get(label), label, nonempty=label != "exclusions")
        for label in ("allowed_write_paths", "allow_dirty_paths", "generated_paths"):
            for value in text_list(scope.get(label), label):
                if self.pattern(value) != value:
                    raise ScopeError("Use normalized scope paths")
        for value in scope["allow_dirty_paths"]:
            if value.endswith("/**") or not matches(value, scope["allowed_write_paths"]):
                raise ScopeError("Dirty-path permission must name an allowed file explicitly")
        for value in scope["generated_paths"]:
            if (not value.endswith("/**") or Path(value[:-3]).name not in ARTIFACT_DIRS
                    or value.startswith((".ai/", ".codex/", ".agents/", "scripts/hooks/"))):
                raise ScopeError("Generated exemptions must name a recognized artifact directory/**")
        operations = text_list(scope.get("operations"), "operations")
        if not set(operations).issubset(SENSITIVE):
            raise ScopeError("Unknown operation authorization")
        if version == 2:
            pass  # SPEC/PLAN own acceptance criteria and TDD; text fields cannot prove either.
        elif scope.get("change_kind") == "code":
            tdd = scope.get("tdd")
            if not isinstance(tdd, dict) or any(
                    not isinstance(tdd.get(stage), str) or not tdd[stage].strip()
                    for stage in ("red", "green", "refactor")):
                raise ScopeError("Code changes require explicit Red/Green/Refactor steps in the plan")
        elif scope.get("change_kind") == "documentation":
            if not isinstance(scope.get("documentation_checks"), str) or not scope["documentation_checks"].strip():
                raise ScopeError("Documentation-only changes require meaningful checks")
        else:
            raise ScopeError("change_kind must be code or documentation")
        for label in ("spec", "plan"):
            path = self.path(scope[label])
            if path != scope[label] or not (self.root / path).is_file():
                raise ScopeError(label + " must reference an existing repository-relative file")
        if not isinstance(scope.get("allowed_commands"), list):
            raise ScopeError("allowed_commands must be a list")
        for command in scope["allowed_commands"]:
            if not isinstance(command, dict):
                raise ScopeError("Each command must be an object")
            argv = text_list(command.get("argv"), "command argv", nonempty=True)
            if self_approval(argv):
                raise Forbidden("Agents cannot approve their own task")
            operation = command.get("operation")
            if operation not in OPERATIONS:
                raise ScopeError("Unknown command operation")
            if operation in SENSITIVE and operation not in operations:
                raise ScopeError("Command needs explicit " + operation + " authorization")
            implied = git_operation(argv)
            if implied in SENSITIVE and (implied != operation or implied not in operations):
                raise ScopeError("Git delivery needs correctly classified explicit authorization")
            cwd = command.get("cwd")
            if self.path(cwd, allow_root=True) != cwd:
                raise ScopeError("Use a normalized repository-relative command cwd")
        return scope

    def scope_revision(self, relative):
        relative = self.path(relative)
        scope = self.validate_scope(read_json(self.root / relative))
        revision = {"scope": digest(self.root / relative)}
        revision.update({label: digest(self.root / scope[label]) for label in ("spec", "plan")})
        return scope, revision

    def snapshot(self, generated):
        # Include ignored files too: an ignored path must not silently bypass the
        # post-action check. Only approved generated directories are exempt.
        names = self.git("ls-files", "-z", "--cached", "--others").split("\0")
        result = {}
        for name in sorted(set(names) - {""}):
            if matches(name, generated):
                continue
            target = self.root / name
            try:
                metadata = target.lstat()
            except FileNotFoundError:
                continue
            if stat.S_ISLNK(metadata.st_mode):
                content = hashlib.sha256(os.readlink(str(target)).encode()).hexdigest()
                kind = "symlink"
            elif stat.S_ISREG(metadata.st_mode):
                content, kind = digest(target), "file"
            else:
                raise ScopeError("Unsupported workspace entry: " + name)
            result[name] = [kind, stat.S_IMODE(metadata.st_mode), content]
        return result

    def dirty_paths(self):
        entries = iter(self.git("status", "--porcelain=v1", "-z", "--untracked-files=all").split("\0"))
        paths = set()
        for entry in entries:
            if not entry:
                continue
            paths.add(entry[3:])
            if "R" in entry[:2] or "C" in entry[:2]:
                paths.add(next(entries))
        paths.update(self.git("ls-files", "-z", "--others", "--ignored", "--exclude-standard").split("\0"))
        return sorted(paths - {""})

    def head(self):
        try:
            return self.git("rev-parse", "--verify", "HEAD").strip()
        except ScopeError:
            return None  # Unborn repositories are valid.

    def approve(self, relative):
        """Write a receipt; call only from the human CLI or disposable test fixtures.

        This method is not an authentication boundary. Protect Git metadata with
        the runner's sandbox; the agent must never invoke approval, even via Python.
        """
        relative = self.path(relative)
        scope, revision = self.scope_revision(relative)
        receipt = {"version": 1, "root": str(self.root), "session_id": self.session_id,
                   "scope_path": relative, "revision": revision,
                   "baseline": self.snapshot(scope["generated_paths"]),
                   "dirty_paths": self.dirty_paths(), "head": self.head()}
        self.receipt_path.parent.mkdir(mode=0o700, parents=True, exist_ok=True)
        descriptor, name = tempfile.mkstemp(dir=str(self.receipt_path.parent), prefix="receipt-")
        try:
            with os.fdopen(descriptor, "w", encoding="utf-8") as stream:
                json.dump(receipt, stream, sort_keys=True)
            os.replace(name, str(self.receipt_path))
        finally:
            if os.path.exists(name):
                os.unlink(name)

    def approval(self):
        if not self.receipt_path.exists():
            raise ScopeError("No approved scope for session " + self.session_id)
        receipt = read_json(self.receipt_path)
        if (receipt.get("version") != 1 or receipt.get("root") != str(self.root)
                or receipt.get("session_id") != self.session_id
                or not isinstance(receipt.get("baseline"), dict)
                or not isinstance(receipt.get("dirty_paths"), list)):
            raise ScopeError("Approval receipt is invalid")
        scope, revision = self.scope_revision(receipt.get("scope_path"))
        if receipt.get("revision") != revision:
            raise ScopeError("Scope, specification, or plan changed; request approval of the amendment")
        return scope, receipt

    def write_permission(self, path, scope, receipt):
        path = self.path(path)
        if not matches(path, scope["allowed_write_paths"]):
            return Decision("ASK", "Path is outside approved scope: " + path)
        if path in receipt["dirty_paths"] and path not in scope["allow_dirty_paths"]:
            return Decision("ASK", "Preserve pre-existing edits; obtain permission to edit: " + path)
        operation = None
        parts = PurePosixPath(path).parts
        if (".codex" in parts or Path(path).name == "AGENTS.md"
                or path.startswith((".agents/", ".ai/rules/", ".ai/templates/", "scripts/hooks/"))
                or path == "scripts/scope_guard.py"):
            operation = "policy"
        elif "migration" in parts and path.endswith(".sql"):
            operation = "migration"
        elif Path(path).name in {"pom.xml", "build.gradle", "build.gradle.kts", "package.json",
                                 "package-lock.json", "requirements.txt", "pyproject.toml"}:
            operation = "dependency"
        if operation and operation not in scope["operations"]:
            return Decision("ASK", "Explicit " + operation + " authorization required for: " + path)
        return Decision("AUTO", "Approved path: " + path)

    def check_changes(self):
        try:
            scope, receipt = self.approval()
            current = self.snapshot(scope["generated_paths"])
            for path in sorted(set(current) | set(receipt["baseline"])):
                if current.get(path) != receipt["baseline"].get(path):
                    decision = self.write_permission(path, scope, receipt)
                    if decision.mode != "AUTO":
                        return decision
            if (self.head() != receipt.get("head")
                    and not HISTORY_OPERATIONS.intersection(scope["operations"])):
                return Decision("ASK", "HEAD changed without explicit history authorization")
            return Decision("AUTO", "Workspace changes match the approved scope")
        except Forbidden as error:
            return Decision("DENY", str(error))
        except (ScopeError, OSError) as error:
            return Decision("ASK", str(error))

    def read_command(self, tokens, cwd):
        """Small, deliberately conservative inspection vocabulary."""
        program, args = tokens[0], tokens[1:]
        if program == "git":
            no_pager = args[:1] == ["--no-pager"]
            if no_pager:
                args = args[1:]
            if no_pager and args[:1] == ["diff"]:
                return {"--no-ext-diff", "--no-textconv"}.issubset(args[1:]) and all(
                    item in {"--no-ext-diff", "--no-textconv", "--stat", "--name-only", "--check", "--cached"}
                    for item in args[1:])
            if no_pager and args[:1] == ["log"]:
                return len(args) >= 2 and args[1] == "--oneline" and all(
                    re.fullmatch(r"-[1-9]\d{0,2}", item) for item in args[2:])
            return args == ["branch", "--show-current"] or (
                bool(args) and args[0] == "status" and all(item in {
                    "--short", "--branch", "--porcelain", "--porcelain=v1", "-s", "-b"}
                    for item in args[1:]))
        if program == "pwd":
            return not args
        if program == "cat":
            files = args[1:] if args[:1] == ["--"] else args
            if not files or any(item.startswith("-") for item in files):
                return False
            for path in files:
                self.path(path, cwd)
            return True
        if program == "sed" and len(args) >= 3 and args[0] == "-n" and re.fullmatch(r"\d+(?:,(?:\d+|\$))?p", args[1]):
            if any(path.startswith("-") for path in args[2:]):
                return False
            for path in args[2:]:
                self.path(path, cwd)
            return True
        if program == "rg" and args[:1] == ["--files"]:
            for path in args[1:]:
                if path.startswith("-"):
                    return False
                self.path(path, cwd, allow_root=True)
            return True
        if program == "rg" and args[:2] == ["--no-config", "-n"] and len(args) >= 3:
            if args[2].startswith("-"):
                return False
            for path in args[3:]:
                if path.startswith("-"):
                    return False
                self.path(path, cwd, allow_root=True)
            return True
        return False

    def patch_paths(self, patch, cwd):
        if not isinstance(patch, str):
            raise Forbidden("Expected a structured apply_patch command")
        lines = patch.strip().splitlines()
        if not lines or lines[0] != "*** Begin Patch" or lines[-1] != "*** End Patch":
            raise Forbidden("Malformed patch")
        paths = []
        prefixes = ("*** Add File: ", "*** Update File: ", "*** Delete File: ", "*** Move to: ")
        for line in lines[1:-1]:
            for prefix in prefixes:
                if line.startswith(prefix):
                    paths.append(self.path(line[len(prefix):], cwd))
                    break
            else:
                if line.startswith("*** ") and line != "*** End of File":
                    raise Forbidden("Unsupported patch directive")
        if not paths:
            raise Forbidden("Patch has no file paths")
        return paths

    def event_input(self, event):
        if not isinstance(event, dict) or event.get("session_id") != self.session_id:
            raise ScopeError("Event is not bound to this session")
        tool, data = event.get("tool_name"), event.get("tool_input")
        if not isinstance(data, dict):
            raise ScopeError("Unsupported tool arguments")
        cwd = event.get("cwd", str(self.root))
        cwd = data.get("cwd", data.get("workdir", cwd)) or cwd
        cwd = str(self.root / self.path(cwd, allow_root=True))
        return tool, data, cwd

    def inspection_event(self, event):
        try:
            tool, data, cwd = self.event_input(event)
            return tool in ("Bash", "exec_command") and self.read_command(
                command_tokens(data.get("command", data.get("cmd"))), cwd)
        except (ScopeError, OSError):
            return False

    def check_event(self, event):
        try:
            tool, data, cwd = self.event_input(event)
            if tool in ("Bash", "exec_command"):
                tokens = command_tokens(data.get("command", data.get("cmd")))
                if self_approval(tokens):
                    raise Forbidden("Agents cannot invoke scope approval; a human must approve in a separate terminal")
                if self.read_command(tokens, cwd):
                    return Decision("AUTO", "Read-only repository inspection")
                scope, receipt = self.approval()
                pending = self.check_changes()
                if pending.mode != "AUTO":
                    return pending
                directory = self.path(cwd, allow_root=True)
                for command in scope["allowed_commands"]:
                    if tokens == command["argv"] and directory == command["cwd"]:
                        return Decision("AUTO", "Exact command covered by user approval")
                return Decision("ASK", "Command or working directory is outside approved scope")
            if tool == "apply_patch":
                paths = self.patch_paths(data.get("command"), cwd)
                scope, receipt = self.approval()
                pending = self.check_changes()
                if pending.mode != "AUTO":
                    return pending
                for path in paths:
                    decision = self.write_permission(path, scope, receipt)
                    if decision.mode != "AUTO":
                        return decision
                return Decision("AUTO", "Every patch path is approved")
            return Decision("ASK", "Unrecognized tool; explicitly review its effects before use: " + str(tool))
        except Forbidden as error:
            return Decision("DENY", str(error))
        except (ScopeError, OSError) as error:
            return Decision("ASK", str(error))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", default=str(Path(__file__).resolve().parents[1]))
    sub = parser.add_subparsers(dest="action", required=True)
    for action in ("approve", "check"):
        command = sub.add_parser(action)
        command.add_argument("--session", required=True, help="Exact session_id reported by Codex")
        if action == "approve":
            command.add_argument("--scope", required=True)
    args = parser.parse_args()
    try:
        guard = ScopeGuard(args.repo, args.session)
        if args.action == "check":
            decision = guard.check_changes()
            print(json.dumps(decision._asdict()))
            return 0 if decision.mode == "AUTO" else 2
        if not sys.stdin.isatty():
            raise ScopeError("Approval requires a human in an interactive terminal; agents must not run this command")
        scope, revision = guard.scope_revision(args.scope)
        print(json.dumps({"scope": scope, "revision": revision, "session": args.session}, indent=2))
        phrase = "approve " + scope["task_id"] + " " + revision["scope"][:12]
        if input("After reviewing the spec, plan, and scope, type '" + phrase + "': ") != phrase:
            raise ScopeError("Approval cancelled")
        # Recheck after the prompt so concurrent edits cannot receive stale approval.
        if guard.scope_revision(args.scope)[1] != revision:
            raise ScopeError("Documents changed during approval; review again")
        guard.approve(args.scope)
        print("Approved for session " + args.session + ". Commit/push require their own explicit scope authorization.")
        return 0
    except (ScopeError, OSError) as error:
        print("Scope check: " + str(error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main())
