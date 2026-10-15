# Scope and approval rules

The [harness skill](../../.agents/skills/harness/SKILL.md) owns the workflow,
review format, TDD, and proportional documentation. These rules own approval
boundaries and executable enforcement for every MealIO component.

## Approval decisions

| Status | Action |
| --- | --- |
| AUTO | Continue inspection and work within the approved task, including related tests and verification. |
| ASK | Pause the affected action for missing/ambiguous authorization, material scope changes, or unapproved consequential operations. |
| DENY | Reject fabricated approval, unauthorized destruction of user work, path traversal, and direct Git-internal writes. |

Approve the combined SPEC/PLAN once. Approval applies to the clearly identified
proposal and any stated limits; no special approval phrase is required. Approval
of a SPEC alone approves the behavior contract, not implementation, even if an
earlier plan exists. Present the combined proposal before starting implementation.
If the user clearly approves implementation of that proposal, continue tests,
fixes, verification, and review within scope without per-file or per-command
approval requests. Follow-up questions do not revoke unchanged approval.

When the approval target is genuinely ambiguous, clarify before dependent writes;
continue useful read-only discovery. Material scope changes need approval only
for the changed boundary. Approvals do not cover unrelated future work. Preserve
pre-existing edits; never revert unexpected changes automatically.

Dependency, migration, security, infrastructure, and harness-policy changes must
be explicit in the approved scope. Commit, push, merge/rebase/pull, deployment,
and persistent-database operations require explicit authorization for those
operations. Starting an application can apply migrations and needs authorization
when it targets persistent data. Ordinary implementation approval does not imply
these permissions. Agents cannot issue their own approval receipts.

## Review mode

Select this mode explicitly with `--mode review` in registered commands. The
actual registration is authoritative: omitted mode means strict, and editing this
guide does not activate a configuration change. In review mode, approval is
recorded in conversation; no receipt, JSON manifest, or execution diary is required.
SessionStart supplies guidance. Other hook events return `{}`, leaving native
tool permissions unchanged. Approval and task scope are instruction-level
controls, checked through the actual diff, tests, and review; the hook does not
authenticate chat approval or prevent writes outside a task mechanically.

Preview and review-only requests remain read-only. This mode does not authorize
implementation merely because a hook returns no permission override.

## Optional: strict mode

Use strict mode when executable path/command enforcement is required. Selecting
or changing mode is an explicit policy decision: set every registered command
in `.codex/hooks.json` to `--mode strict`, review/trust the configuration, and
verify actual event delivery. Do not switch modes to get around a blocked action.
Omitting the adapter mode preserves strict behavior; invalid modes block.
Strict mode never falls back to review mode when approval is absent or invalid.

Prepare one SPEC, one PLAN, and a manifest using the [template](../templates/scope.json).
Version 2 contains only task ID, document references, allowed write/dirty/generated
paths, exact command argv/cwd/operation, and sensitive operations. Version 1 remains
readable for compatibility; old task manifests are historical boundaries, not new
approval. Acceptance criteria and Red/Green/Refactor belong in SPEC/PLAN, not JSON.

Use exact files or narrow `directory/**` boundaries. Explicitly name authorized
pre-existing dirty files. Generated exemptions cover artifact directories such as
`target/**`, not components or policy trees. Paths and sensitive operations are
both required. Policy includes `AGENTS.md`, `.agents/`, `.codex/`, scope rules,
templates, and checker/hook source.

In a human terminal, record the reviewed scope for the actual runtime session:

```bash
python3 -B scripts/scope_guard.py approve --scope .ai/tasks/<task-id>.scope.json --session <session-id>
```

Replace placeholders. Agents must never run this command, including via a PTY,
module invocation, or Python. Receipts live under Git's common directory, bind
session/document revisions and the starting workspace, and must be sandbox-protected.
Strict document amendments need renewed approval; keep routine progress out of
bound documents. Prepare documents before activation or through a separately
authorized preparation process; strict mode does not solve that bootstrap step.

## Verification and limits

Run tooling tests from the repository root:

```bash
python3 -B -m unittest discover -s scripts/tests -p 'test_scope_guard.py' -v
```

Strict mode checks supported shell/patch tools, exact commands, existing edits,
and workspace changes. Unknown tools/commands return ASK; shell composition is
unsupported. ASK/DENY block supported pre-tool actions. Post-tool checks cannot
undo actions; Stop reports violations without a continuation loop.

Neither mode proves TDD, business correctness, or database/remote effects. Strict
hooks can miss unsupported tool paths, interactive inputs, startup failures, and
timeouts. Approved scripts/plugins remain trusted executable code. Test actual
read/allowed/denied events in disposable fixtures; registration and subprocess
tests do not prove live client activation. Use sandbox permissions and honest
PASS/FAIL/BLOCKED reporting alongside the hooks.
