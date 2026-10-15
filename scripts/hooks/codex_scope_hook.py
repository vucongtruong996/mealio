#!/usr/bin/env python3
"""Translate Codex lifecycle events into scope decisions; never execute actions."""

import argparse
import json
from pathlib import Path
import sys

# -B is also present in hook registration; keep imports artifact-free when called
# directly so a policy check cannot create an unexpected __pycache__ directory.
sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))


def response(event, decision, inspection=False):
    label = decision.mode + ": " + decision.reason
    if event == "PreToolUse":
        return {"hookSpecificOutput": {
            "hookEventName": event,
            "permissionDecision": "allow" if decision.mode == "AUTO" else "deny",
            "permissionDecisionReason": label}}
    if event == "PostToolUse":
        if decision.mode == "AUTO":
            return {}
        output = {"hookSpecificOutput": {"hookEventName": event, "additionalContext":
            label + ". Pause mutations, report the difference, and request scope review. Do not revert user files."}}
        if inspection:
            output["systemMessage"] = label  # Keep diagnostic tool results visible.
        else:
            output.update({"decision": "block", "reason": label})
        return output
    if event == "SessionStart":
        return {"hookSpecificOutput": {"hookEventName": event, "additionalContext":
            "Scope approval status: " + label +
            ". Read .ai/rules/scope.md. Follow specification → plan with TDD → user approval → implementation. "
            "Do not self-approve or commit/push without explicit authorization."}}
    if event == "Stop":
        # decision:block would automatically create another turn. Report the
        # violation without an endless continuation loop requiring human input.
        return {} if decision.mode == "AUTO" else {
            "systemMessage": "Scope verification " + label,
            "continue": False, "stopReason": label + ". Report this as blocked, not complete."}
    return {"systemMessage": label}


def main():
    event_name = "PreToolUse"  # A malformed pre-tool payload must block safely.
    try:
        parser = argparse.ArgumentParser(add_help=False)
        parser.add_argument("--repo", required=True)
        # Existing registrations without a mode retain strict enforcement.
        parser.add_argument("--mode", choices=("review", "strict"), default="strict")
        options = parser.parse_args()
        event = json.load(sys.stdin)
        if not isinstance(event, dict):
            raise ValueError("Expected an event object")
        stage = event.get("hook_event_name", "PreToolUse")
        if stage not in {"SessionStart", "PreToolUse", "PostToolUse", "Stop"}:
            raise ValueError("Unsupported hook event")
        event_name = stage
        if options.mode == "review":
            output = {}
            if event_name == "SessionStart":
                output = {"hookSpecificOutput": {"hookEventName": event_name, "additionalContext":
                    "MealIO review mode: read AGENTS.md and .agents/skills/harness/SKILL.md. "
                    "Present detailed SPEC/PLAN for chat approval, then implement with TDD, verify, "
                    "and present the diff and actual results. Reuse unchanged task approval. "
                    "Explain routine process only on request. Scope changes and consequential operations "
                    "need explicit authorization. This hook does not authenticate approval or enforce "
                    "task paths; sandbox permissions remain applicable. Never fabricate receipts "
                    "or commit/push/merge/deploy without explicit authorization."}}
            # No permissionDecision: leave native permissions intact. No receipt
            # lookup, workspace writes, or strict-to-review fallback occurs here.
            print(json.dumps(output))
            return 0
        from scope_guard import ScopeGuard
        guard = ScopeGuard(options.repo, event.get("session_id"))
        inspection = event_name == "PostToolUse" and guard.inspection_event(event)
        if not guard.receipt_path.exists() and (inspection or event_name == "Stop"):
            print("{}")  # Preview sessions need no implementation approval.
            return 0
        decision = guard.check_event(event) if event_name == "PreToolUse" else guard.check_changes()
        print(json.dumps(response(event_name, decision, inspection)))
    except (Exception, SystemExit):
        # Avoid leaking payloads, file contents, or exception details. Return an
        # explicit denial instead of crashing into Codex's fail-open hook behavior.
        class Failure:
            mode = "DENY"
            reason = "Scope checker failed; stop mutations and inspect the checker before continuing"
        print(json.dumps(response(event_name, Failure())))
    return 0


if __name__ == "__main__":
    sys.exit(main())
