"""Behavior tests use disposable Git repositories, never MealIO's Git state."""

import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


REPO = Path(__file__).resolve().parents[2]
CHECKER = REPO / "scripts/scope_guard.py"
ADAPTER = REPO / "scripts/hooks/codex_scope_hook.py"
if CHECKER.exists():
    sys.path.insert(0, str(CHECKER.parent))
    from scope_guard import ScopeGuard, ScopeError
else:
    ScopeGuard = None
    ScopeError = ValueError


class ScopeGuardTest(unittest.TestCase):
    def setUp(self):
        self.assertIsNotNone(ScopeGuard, "Scope checker has not been implemented")
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name).resolve()
        self.git("init", "-q")
        for name in ("allowed.txt", "other.txt", "existing.txt", "spec.md", "plan.md"):
            self.write(name, "baseline\n")
        self.git("add", ".")
        self.git("-c", "user.name=Fixture", "-c", "user.email=fixture@example.invalid",
                 "commit", "-qm", "fixture baseline")
        self.scope = {
            "version": 1,
            "task_id": "fixture",
            "objective": "Change only the approved fixture behavior",
            "components": ["engineering"],
            "acceptance_criteria": ["SC01: restrict task changes"],
            "spec": "spec.md",
            "plan": "plan.md",
            "change_kind": "code",
            "allowed_write_paths": ["allowed.txt", "new.txt", "tests/**"],
            "allow_dirty_paths": [],
            "generated_paths": ["build-output/**"],
            "allowed_commands": [{
                "argv": ["python3", "-B", "-m", "unittest"],
                "cwd": ".", "operation": "test"
            }],
            "operations": [],
            "exclusions": ["No changes to another component"],
            "tdd": {"red": "Run the new test and observe a behavior failure",
                    "green": "Implement the minimum behavior and rerun",
                    "refactor": "Simplify while the regression suite passes"}
        }
        self.scope_path = ".ai/tasks/fixture.scope.json"
        self.save_scope()
        self.guard = ScopeGuard(self.root, "session-one")

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=str(self.root)).decode().strip()

    def write(self, path, content):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content)

    def save_scope(self):
        self.write(self.scope_path, json.dumps(self.scope))

    def approve(self):
        self.save_scope()
        self.guard.approve(self.scope_path)

    def event(self, name, tool_input):
        return {"hook_event_name": "PreToolUse", "session_id": "session-one",
                "cwd": str(self.root), "tool_name": name, "tool_input": tool_input}

    def shell(self, command, cwd=None):
        return self.guard.check_event(self.event("Bash", {
            "command": command, "cwd": str(cwd or self.root)
        }))

    def patch(self, path="allowed.txt", extra=""):
        return self.guard.check_event(self.event("apply_patch", {"command":
            "*** Begin Patch\n*** Update File: " + path +
            "\n@@\n-baseline\n+changed\n" + extra + "*** End Patch\n"}))

    def test_read_only_inspection_without_approval(self):
        self.assertEqual("AUTO", self.shell("git status --short --branch").mode)
        self.assertEqual("AUTO", self.shell("cat spec.md").mode)
        self.assertEqual("AUTO", self.shell("rg --files").mode)

    def test_mutation_without_approval_asks(self):
        self.assertEqual("ASK", self.patch().mode)

    def test_approved_patch_runs_automatically(self):
        self.approve()
        self.assertEqual("AUTO", self.patch().mode)

    def test_other_component_requires_scope_amendment(self):
        self.approve()
        self.assertEqual("ASK", self.patch("other.txt").mode)

    def test_absolute_in_root_patch_path_is_supported(self):
        self.approve()
        self.assertEqual("AUTO", self.patch(str(self.root / "allowed.txt")).mode)

    def test_traversal_is_denied(self):
        self.approve()
        self.assertEqual("DENY", self.patch("../escape.txt").mode)

    def test_git_internal_writes_are_denied(self):
        self.approve()
        self.assertEqual("DENY", self.patch(".git/config").mode)

    def test_symlink_escape_is_denied(self):
        self.approve()
        os.symlink(self.root.parent, self.root / "escape")
        self.assertEqual("DENY", self.patch("escape/outside.txt").mode)

    def test_patch_checks_every_file(self):
        self.approve()
        extra = "*** Add File: other-service/new.txt\n+unexpected\n"
        self.assertEqual("ASK", self.patch(extra=extra).mode)

    def test_rename_checks_destination(self):
        self.approve()
        extra = "*** Move to: other-service/moved.txt\n"
        self.assertEqual("ASK", self.patch(extra=extra).mode)

    def test_malformed_patch_is_denied(self):
        self.assertEqual("DENY", self.guard.check_event(
            self.event("apply_patch", {"command": "not a patch"})).mode)

    def test_exact_test_command_is_allowed(self):
        self.approve()
        self.assertEqual("AUTO", self.shell("python3 -B -m unittest").mode)
        self.assertEqual("ASK", self.shell("python3 -B -m unittest other_test").mode)

    def test_command_directory_is_bound_to_approval(self):
        self.approve()
        (self.root / "subdir").mkdir()
        self.assertEqual("ASK", self.shell("python3 -B -m unittest", self.root / "subdir").mode)

    def test_shell_composition_cannot_bypass_command_check(self):
        self.approve()
        for command in ("git status; touch other.txt", "git status && rm other.txt",
                        "cat $(touch other.txt)", "git status > other.txt",
                        "git status\ntouch other.txt", "python3 -B -m unittest &"):
            with self.subTest(command=command):
                self.assertNotEqual("AUTO", self.shell(command).mode)

    def test_read_tools_cannot_execute_programs_or_write(self):
        for command in ("rg --pre touch pattern .", "sed -n 'w other.txt' allowed.txt",
                        "sed -n 1p -i allowed.txt", "sed -n 1p -f other.txt allowed.txt",
                        "git diff --output=other.txt", "git -c alias.x=bad x"):
            with self.subTest(command=command):
                self.assertNotEqual("AUTO", self.shell(command).mode)

    def test_unknown_tools_and_commands_ask(self):
        self.approve()
        self.assertEqual("ASK", self.shell("python3 -c 'print(1)'").mode)
        self.assertEqual("ASK", self.guard.check_event(
            self.event("mcp__filesystem__write_file", {"path": "allowed.txt"})).mode)

    def test_commit_and_push_require_specific_authorization(self):
        self.approve()
        self.assertEqual("ASK", self.shell("git commit -m approved").mode)
        self.assertEqual("ASK", self.shell("git push origin main").mode)

    def test_commit_allowlist_alone_does_not_authorize_commit(self):
        self.scope["allowed_commands"].append({
            "argv": ["git", "commit", "-m", "approved"], "cwd": ".", "operation": "commit"})
        with self.assertRaises(ScopeError):
            self.approve()

    def test_git_global_options_cannot_hide_delivery_operations(self):
        for argv in (["git", "--no-pager", "commit", "-m", "hidden"],
                     ["/usr/bin/git", "push", "origin", "main"],
                     ["git", "-C", ".", "commit", "-m", "hidden"]):
            with self.subTest(argv=argv):
                self.scope["allowed_commands"] = [{"argv": argv, "cwd": ".", "operation": "test"}]
                with self.assertRaises(ScopeError):
                    self.approve()

    def test_explicit_commit_authorization_allows_only_that_command(self):
        self.scope["operations"] = ["commit"]
        self.scope["allowed_commands"].append({
            "argv": ["git", "commit", "-m", "approved"], "cwd": ".", "operation": "commit"})
        self.approve()
        self.assertEqual("AUTO", self.shell("git commit -m approved").mode)
        self.assertEqual("ASK", self.shell("git commit -m different").mode)
        self.assertEqual("ASK", self.shell("git push origin main").mode)

    def test_agent_cannot_invoke_approval_command(self):
        self.approve()
        self.assertEqual("DENY", self.shell(
            "python3 scripts/scope_guard.py approve --scope x --session y").mode)

    def test_module_form_self_approval_is_denied(self):
        self.assertEqual("DENY", self.shell("python3 -m scope_guard approve --scope x --session y").mode)

    def test_untracked_ignored_files_are_protected_as_existing_edits(self):
        self.write(".gitignore", "ignored-existing.txt\n")
        self.write("ignored-existing.txt", "user's existing ignored file")
        self.scope["allowed_write_paths"].append("ignored-existing.txt")
        self.approve()
        self.assertEqual("ASK", self.patch("ignored-existing.txt").mode)

    def test_read_only_diff_and_search_do_not_need_task_approval(self):
        self.assertEqual("AUTO", self.shell("git --no-pager diff --no-ext-diff --no-textconv --stat").mode)
        self.assertEqual("AUTO", self.shell("git --no-pager log --oneline -5").mode)
        self.assertEqual("AUTO", self.shell("rg --no-config -n baseline spec.md").mode)

    def test_scope_spec_and_plan_changes_invalidate_approval(self):
        for path in (self.scope_path, "spec.md", "plan.md"):
            with self.subTest(path=path):
                self.approve()
                old = (self.root / path).read_text()
                self.write(path, old + "\n")
                self.assertEqual("ASK", self.patch().mode)
                self.write(path, old)

    def test_approval_is_bound_to_session(self):
        self.approve()
        other = ScopeGuard(self.root, "another-session")
        self.assertEqual("ASK", other.check_event(self.event(
            "apply_patch", {"command": "*** Begin Patch\n*** Add File: new.txt\n+x\n*** End Patch"})).mode)

    def test_new_modified_deleted_and_mode_changes_are_detected(self):
        self.approve()
        self.write("allowed.txt", "changed")
        self.write("new.txt", "new")
        self.assertEqual("AUTO", self.guard.check_changes().mode)
        self.write("other.txt", "unexpected")
        self.assertEqual("ASK", self.guard.check_changes().mode)

    def test_unexpected_deletion_is_detected(self):
        self.approve()
        (self.root / "other.txt").unlink()
        self.assertEqual("ASK", self.guard.check_changes().mode)

    def test_unexpected_mode_change_is_detected(self):
        self.approve()
        (self.root / "other.txt").chmod(0o755)
        self.assertEqual("ASK", self.guard.check_changes().mode)

    def test_ignored_unexpected_file_is_detected(self):
        self.write(".gitignore", "ignored/\n")
        self.approve()
        self.write("ignored/unexpected.txt", "unexpected")
        self.assertEqual("ASK", self.guard.check_changes().mode)

    def test_generated_output_is_exempt_but_cannot_hide_source_changes(self):
        self.approve()
        self.write("build-output/report.xml", "generated")
        self.assertEqual("AUTO", self.guard.check_changes().mode)
        self.write("other.txt", "unexpected")
        self.assertEqual("ASK", self.guard.check_changes().mode)

    def test_existing_dirty_files_are_preserved_and_protected(self):
        self.write("allowed.txt", "user's existing change")
        self.approve()
        self.assertEqual("AUTO", self.guard.check_changes().mode)
        self.assertEqual("ASK", self.patch().mode)
        self.write("allowed.txt", "overwritten")
        self.assertEqual("ASK", self.guard.check_changes().mode)

    def test_explicit_dirty_path_permission_is_required(self):
        self.write("allowed.txt", "user's existing change")
        self.scope["allow_dirty_paths"] = ["allowed.txt"]
        self.approve()
        self.assertEqual("AUTO", self.patch().mode)

    def test_pending_scope_violation_blocks_next_mutation(self):
        self.approve()
        self.write("other.txt", "unexpected")
        self.assertEqual("ASK", self.patch().mode)
        self.assertEqual("AUTO", self.shell("git status --short").mode)

    def test_sensitive_file_requires_operation_and_path_permission(self):
        for path, operation in (("pom.xml", "dependency"),
                                ("service/src/main/resources/db/migration/V1__x.sql", "migration"),
                                ("AGENTS.md", "policy")):
            with self.subTest(path=path):
                self.scope["allowed_write_paths"].append(path)
                self.approve()
                self.assertEqual("ASK", self.patch(path).mode)
                self.scope["operations"].append(operation)
                self.approve()
                self.assertEqual("AUTO", self.patch(path).mode)

    def test_overbroad_traversal_and_git_paths_cannot_be_approved(self):
        for path in ("**", "../escape/**", ".git/**", "/tmp/escape.txt"):
            with self.subTest(path=path):
                self.scope["allowed_write_paths"] = [path]
                with self.assertRaises(ScopeError):
                    self.approve()

    def test_generated_paths_cannot_hide_policy_or_whole_components(self):
        for path in (".ai/**", "scripts/**", "mealio-menu-service/**", "**",
                     ".ai/rules/build/**", ".codex/target/**", "scripts/hooks/build/**"):
            with self.subTest(path=path):
                self.scope["generated_paths"] = [path]
                with self.assertRaises(ScopeError):
                    self.approve()

    def test_code_scope_requires_explicit_tdd_plan(self):
        del self.scope["tdd"]
        with self.assertRaises(ScopeError):
            self.approve()

    def minimal_scope(self):
        self.scope = {key: value for key, value in self.scope.items() if key in {
            "task_id", "spec", "plan", "allowed_write_paths", "allow_dirty_paths",
            "generated_paths", "allowed_commands", "operations"}}
        self.scope["version"] = 2

    def test_v2_manifest_enforces_boundaries_without_duplicating_spec_or_tdd(self):
        self.minimal_scope()
        self.approve()
        self.assertEqual("AUTO", self.patch().mode)
        self.assertEqual("AUTO", self.shell("python3 -B -m unittest").mode)
        self.assertEqual("ASK", self.patch("other.txt").mode)

    def test_v2_manifest_still_binds_spec_and_plan_revisions(self):
        self.minimal_scope()
        self.approve()
        self.write("plan.md", "amended implementation boundary\n")
        self.assertEqual("ASK", self.patch().mode)

    def test_v2_manifest_requires_existing_spec_and_plan(self):
        self.minimal_scope()
        self.scope["spec"] = "missing.md"
        with self.assertRaises(ScopeError):
            self.approve()

    def test_skill_changes_require_explicit_policy_authorization(self):
        path = ".agents/skills/harness/SKILL.md"
        self.scope["allowed_write_paths"].append(path)
        self.approve()
        self.assertEqual("ASK", self.patch(path).mode)
        self.scope["operations"] = ["policy"]
        self.approve()
        self.assertEqual("AUTO", self.patch(path).mode)

    def test_git_history_commands_cannot_be_classified_as_ordinary_commands(self):
        for operation in ("merge", "rebase", "pull"):
            with self.subTest(operation=operation):
                self.scope["allowed_commands"] = [{
                    "argv": ["git", operation, "origin/main"],
                    "cwd": ".", "operation": "command"}]
                with self.assertRaises(ScopeError):
                    self.approve()

    def test_explicit_history_authorization_allows_only_the_exact_command(self):
        for operation in ("merge", "rebase", "pull"):
            with self.subTest(operation=operation):
                self.scope["operations"] = [operation]
                self.scope["allowed_commands"] = [{
                    "argv": ["git", operation, "origin/main"],
                    "cwd": ".", "operation": operation}]
                self.approve()
                self.assertEqual("AUTO", self.shell("git " + operation + " origin/main").mode)
                self.assertEqual("ASK", self.shell("git " + operation + " another-branch").mode)

    def test_review_mode_does_not_override_tool_permissions_or_issue_receipts(self):
        events = (
            self.event("apply_patch", {"command":
                "*** Begin Patch\n*** Update File: allowed.txt\n@@\n-baseline\n+changed\n*** End Patch"}),
            self.event("Bash", {"command": "python3 -B -m unittest"}),
            self.event("mcp__filesystem__write_file", {"path": "allowed.txt"}),
        )
        for event in events:
            with self.subTest(tool=event["tool_name"]):
                output = json.loads(self.run_adapter(event, mode="review").stdout)
                self.assertEqual({}, output)
                self.assertFalse(self.guard.receipt_path.exists())
                self.assertEqual("baseline\n", (self.root / "allowed.txt").read_text())

    def test_review_mode_adds_context_without_a_runtime_session_or_task_documents(self):
        (self.root / "spec.md").unlink()
        (self.root / "plan.md").unlink()
        event = {"hook_event_name": "SessionStart", "cwd": str(self.root)}
        output = json.loads(self.run_adapter(event, mode="review").stdout)
        self.assertEqual("SessionStart", output["hookSpecificOutput"]["hookEventName"])
        self.assertNotIn("permissionDecision", output["hookSpecificOutput"])
        self.assertFalse(self.guard.receipt_path.exists())

    def test_review_mode_tool_and_stop_events_are_quiet_even_with_stale_receipt(self):
        self.approve()
        self.guard.receipt_path.write_text("{stale")
        self.write("other.txt", "existing user change")
        for stage in ("PostToolUse", "Stop"):
            with self.subTest(stage=stage):
                event = {"hook_event_name": stage, "session_id": "session-one"}
                self.assertEqual({}, json.loads(self.run_adapter(event, mode="review").stdout))
                self.assertEqual("{stale", self.guard.receipt_path.read_text())
                self.assertEqual("existing user change", (self.root / "other.txt").read_text())

    def test_explicit_strict_mode_still_blocks_missing_approval(self):
        output = json.loads(self.run_adapter(self.event("apply_patch", {"command":
            "*** Begin Patch\n*** Add File: new.txt\n+x\n*** End Patch"}), mode="strict").stdout)
        self.assertEqual("deny", output["hookSpecificOutput"]["permissionDecision"])
        self.assertFalse(self.guard.receipt_path.exists())

    def test_invalid_mode_never_falls_back_to_review_mode(self):
        event = self.event("Bash", {"command": "python3 -B -m unittest"})
        output = json.loads(self.run_adapter(event, mode="automatic").stdout)
        self.assertEqual("deny", output["hookSpecificOutput"]["permissionDecision"])

    def test_review_mode_blocks_malformed_events(self):
        for event in ("{broken", [], {"hook_event_name": "unknown"}):
            with self.subTest(event=event):
                output = json.loads(self.run_adapter(event, mode="review").stdout)
                self.assertEqual("deny", output["hookSpecificOutput"]["permissionDecision"])

    def test_adapter_formats_ask_as_supported_deny(self):
        result = self.run_adapter(self.event("apply_patch", {"command":
            "*** Begin Patch\n*** Add File: new.txt\n+x\n*** End Patch"}))
        output = json.loads(result.stdout)
        decision = output["hookSpecificOutput"]
        self.assertEqual("deny", decision["permissionDecision"])
        self.assertIn("ASK", decision["permissionDecisionReason"])

    def test_adapter_allows_an_approved_tool(self):
        self.approve()
        result = self.run_adapter(self.event("Bash", {"command": "python3 -B -m unittest"}))
        self.assertEqual("allow", json.loads(result.stdout)["hookSpecificOutput"]["permissionDecision"])

    def test_adapter_blocks_malformed_input(self):
        result = self.run_adapter("{not-json")
        self.assertEqual("deny", json.loads(result.stdout)["hookSpecificOutput"]["permissionDecision"])

    def test_post_tool_reports_violation_without_reverting(self):
        self.approve()
        self.write("other.txt", "unexpected")
        event = self.event("Bash", {"command": "python3 -B -m unittest"})
        event["hook_event_name"] = "PostToolUse"
        output = json.loads(self.run_adapter(event).stdout)
        self.assertEqual("block", output["decision"])
        self.assertEqual("unexpected", (self.root / "other.txt").read_text())

    def test_post_read_without_approval_keeps_inspection_results_available(self):
        event = self.event("Bash", {"command": "cat spec.md"})
        event["hook_event_name"] = "PostToolUse"
        output = json.loads(self.run_adapter(event).stdout)
        self.assertNotIn("decision", output)

    def test_post_read_with_violation_warns_without_hiding_diagnostic_results(self):
        self.approve()
        self.write("other.txt", "unexpected")
        event = self.event("Bash", {"command": "git status --short"})
        event["hook_event_name"] = "PostToolUse"
        output = json.loads(self.run_adapter(event).stdout)
        self.assertNotIn("decision", output)
        self.assertIn("ASK", output["systemMessage"])

    def test_read_only_preview_can_finish_without_approval(self):
        event = {"hook_event_name": "Stop", "session_id": "session-one", "cwd": str(self.root)}
        output = json.loads(self.run_adapter(event).stdout)
        self.assertNotIn("continue", output)
        self.assertNotIn("decision", output)

    def test_stop_reports_violation_without_continuation_loop(self):
        self.approve()
        self.write("other.txt", "unexpected")
        event = {"hook_event_name": "Stop", "session_id": "session-one", "cwd": str(self.root)}
        output = json.loads(self.run_adapter(event).stdout)
        self.assertFalse(output["continue"])
        self.assertIn("ASK", output["systemMessage"])
        self.assertNotIn("decision", output)

    def test_session_start_does_not_self_approve(self):
        event = {"hook_event_name": "SessionStart", "session_id": "session-one", "cwd": str(self.root)}
        output = json.loads(self.run_adapter(event).stdout)
        self.assertIn("approval", output["hookSpecificOutput"]["additionalContext"].lower())
        self.assertFalse(self.guard.receipt_path.exists())

    def test_corrupt_receipt_blocks_mutations(self):
        self.approve()
        self.guard.receipt_path.write_text("{broken")
        self.assertEqual("ASK", self.patch().mode)

    def test_malformed_tool_payload_returns_blocking_adapter_response(self):
        event = self.event("Bash", {"command": "git status", "cwd": ["not", "a path"]})
        output = json.loads(self.run_adapter(event).stdout)
        self.assertEqual("deny", output["hookSpecificOutput"]["permissionDecision"])

    def test_cli_cannot_approve_non_interactively(self):
        result = subprocess.run([sys.executable, "-B", str(CHECKER), "--repo", str(self.root),
                                 "approve", "--scope", self.scope_path, "--session", "session-one"],
                                input="fixture\n", text=True, capture_output=True)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(self.guard.receipt_path.exists())

    def run_adapter(self, event, mode=None):
        command = [sys.executable, "-B", str(ADAPTER), "--repo", str(self.root)]
        if mode is not None:
            command.extend(["--mode", mode])
        result = subprocess.run(command,
                                input=event if isinstance(event, str) else json.dumps(event),
                                text=True, capture_output=True)
        self.assertEqual(0, result.returncode, result.stderr)
        return result


class HookRegistrationTest(unittest.TestCase):
    def test_all_four_stages_register_synchronous_repository_hooks(self):
        config = json.loads((REPO / ".codex/hooks.json").read_text())
        self.assertEqual({"SessionStart", "PreToolUse", "PostToolUse", "Stop"}, set(config["hooks"]))
        for event, groups in config["hooks"].items():
            self.assertEqual(1, len(groups), event)
            if event in ("PreToolUse", "PostToolUse"):
                self.assertEqual("*", groups[0]["matcher"])
            hook = groups[0]["hooks"][0]
            self.assertEqual("command", hook["type"])
            self.assertFalse(hook.get("async", False))
            self.assertIn("git rev-parse --show-toplevel", hook["command"])
            self.assertIn("python3 -B", hook["command"])
            self.assertIn("/scripts/hooks/codex_scope_hook.py", hook["command"])

    def test_registered_commands_use_review_mode_without_permission_overrides(self):
        config = json.loads((REPO / ".codex/hooks.json").read_text())
        for stage, groups in config["hooks"].items():
            with self.subTest(stage=stage):
                command = groups[0]["hooks"][0]["command"]
                result = subprocess.run(command, shell=True, cwd=str(REPO / "mealio-menu-service"),
                    input=json.dumps({"hook_event_name": stage, "session_id": "registration-smoke",
                                      "tool_name": "Bash", "tool_input": {"command": "./mvnw test"}}),
                    text=True, capture_output=True)
                self.assertEqual(0, result.returncode, result.stderr)
                output = json.loads(result.stdout)
                if stage == "SessionStart":
                    self.assertNotIn("permissionDecision", output["hookSpecificOutput"])
                else:
                    self.assertEqual({}, output)


if __name__ == "__main__":
    unittest.main()
