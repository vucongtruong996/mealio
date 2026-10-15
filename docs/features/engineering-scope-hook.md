# MealIO harness and scope hooks

## Contract

**Installation:** Revised harness skill is installed and all four registered hook
commands explicitly select review mode. Registered-command subprocess checks pass;
live client trust and event delivery remain unverified.

Across MealIO components, use **propose → approve → deliver**. The developer
reviews one combined SPEC/PLAN proposal, approves or corrects it, then reviews the
implementation and actual results. The proposal covers outcome/exclusions, source
evidence and assumptions, success/failure examples, affected files and test
commands, and unresolved decisions. The AI owns discovery, TDD, implementation,
verification, and self-review within approval. Routine process explanations are
provided on request. Follow-up prompts do not restart approval or create documents.

Small tasks use conversation-only SPEC/PLAN. Complex work keeps one feature SPEC
and one detailed PLAN, updated in place. SPEC owns behavior and acceptance criteria;
PLAN references those criteria and owns implementation and verification. Detail
follows complexity and risk without an arbitrary word limit. Execution evidence
belongs in the final report or PR summary.
Existing feature contracts and significant decisions remain authoritative.

## Approval and enforcement

The installed default is **review** mode: chat approval is an instruction-level
boundary, supported by sandbox permissions, tests, and diff review. SessionStart
loads workflow guidance; tool and completion hooks return no permission override.
This mode does not authenticate approval or enforce task paths mechanically.

**Strict** mode remains available by explicitly changing the registered mode.
It requires a minimal scope manifest and a human terminal receipt as its approval
channel. Missing or invalid receipts never cause a fallback to review mode.
Agents cannot issue receipts or switch modes to escape a blocked task.

Scope amendments and consequential operations require explicit authorization.
Implementation approval does not imply commit, push, merge, deployment, or
persistent-database execution. Approval mechanics and limitations are documented
once in [scope rules](../../.ai/rules/scope.md).

Approval of the SPEC alone does not authorize implementation from an earlier plan.
Approval of the combined proposal covers ordinary in-scope tests and fixes without
per-file or per-command confirmation. Pause only affected actions when a required
decision, material scope amendment, or operation permission is missing.

Repository claims must be checked against current source and uncertainty stated
explicitly. Delivery reports actual commands/results and outstanding limitations,
separating implementation, verification, and deployment. Required blocked checks
prevent an unqualified completion claim. Instructions do not guarantee error-free
agent behavior; executable hook tests do not establish agent judgment.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| HC01 | Review mode works without task files or receipts and never grants a tool permission override. |
| HC02 | Review hooks create no approval records, execute no requested actions, and produce no routine tool/completion noise. |
| HC03 | Strict mode preserves session/revision binding, exact paths/commands, dirty-file protection, and workspace checks. |
| HC04 | Invalid modes or malformed hook events produce blocking errors, without silently downgrading enforcement. |
| HC05 | Strict manifests support concise version 2 references/boundaries and existing version 1 records. |
| HC06 | Strict policy protection includes agent skills; Git merge/rebase/pull require correctly classified operation authorization. |
| HC07 | Regression tests exercise real adapter subprocesses and configured commands using disposable repositories. |
| HC08 | SPEC/PLAN provide sufficient review detail, with distinct ownership and in-place updates; prompts do not generate redundant records. |
| HC09 | One combined proposal makes outcome, evidence, acceptance examples, file boundaries, commands, and required decisions reviewable without routine document proliferation. |
| HC10 | SPEC-only approval does not start implementation; combined approval continues ordinary in-scope work without repeated confirmation. Preview-only requests cause no writes or builds. |
| HC11 | Delivery distinguishes verified facts from assumptions, fresh from historical results, and implemented from verified/deployed; required blocked checks remain visible. |
| HC12 | Scope expansion pauses only affected work for approval; independent authorized work can continue. |

HC08–HC12 are instruction-level criteria. Review representative scenarios against
the maintained instructions; report that as manual contract review, not an
executable behavioral test or proof of future agent compliance.

## Boundaries and history

Use Python 3.8+ standard-library tooling. This change covers the harness, its
registration/tests, and administrative documentation; no application, database,
dependency, deployment, or unrelated modernization changes.

The original strict implementation recorded 51 passing tests on 2026-10-12.
That is historical evidence. Live Codex hook delivery/trust was not verified.
Current results are reported after checks run, rather than appended as a diary.

[Implementation plan](../../.ai/tasks/scope-hook.md).
