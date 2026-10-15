---
name: harness
description: Develop and review MealIO changes with one reviewable proposal, explicit approval, scoped TDD, and evidence-based delivery.
---

# MealIO engineering harness

Use three steps: **propose → approve → deliver**. Own the engineering work within
approved scope; keep the developer's review focused on behavior, decisions, and evidence.

Read [AGENTS.md](../../../AGENTS.md), applicable nested instructions, and
[scope rules](../../../.ai/rules/scope.md). Use relevant
[project context](../../../MEALIO_PROJECT_CONTEXT.md) and
[documentation](../../../docs/README.md). Preserve existing edits.

## Propose

Inspect current files before describing their behavior. Reuse existing contracts.
Present one combined SPEC/PLAN with:

- **Outcome:** current behavior, desired result, and exclusions.
- **Evidence:** relevant source references; distinguish verified facts, assumptions,
  and unknowns. Resolve uncertainty that affects correctness before acting on it.
- **Acceptance tests:** concrete success/failure examples, including applicable
  compatibility, permissions, data preservation, and concurrency behavior.
- **Plan:** affected files, ordered changes, test levels and exact commands. For
  code, specify meaningful Red failures, minimum Green changes, and regression
  checks. Include rollout/recovery conditions when consequential operations apply.
- **Decisions:** only unresolved choices or risks that affect approval.

Keep enough detail to make the change reviewable; omit irrelevant sections.
Small tasks stay in chat. Complex tasks reuse one SPEC and one PLAN, updated in
place. Link their detail with a short review summary. Do not create execution
logs, per-message documents, or duplicate checklists.

## Approve

Use the scope rules to identify what was approved. Approval of a SPEC alone does
not authorize implementation. After approval of the combined proposal, continue
ordinary work and fixes within its boundary without repeating approval requests.
Pause only affected work for a required decision, material scope change, or missing
operation permission; continue independent authorized work. Preview permits
inspection and proposal only: no file writes, builds, tests, or implementation.
Review-only requests remain read-only unless fixes are authorized. Never fabricate
approval or bypass enforcement.

## Deliver

For executable changes, run a meaningful failing test before production changes,
implement the minimum passing behavior, then refactor and run relevant regressions.
Compilation/environment failures are blockers, not Red evidence. If the proposed
regression already passes, revisit the diagnosis. Documentation changes use document
checks rather than artificial TDD.

Before handoff, review the actual diff, including new files, against acceptance
criteria, failure paths, and scope. Fix defects within approval; report unresolved
issues. Answer follow-up questions without restarting the workflow.

Report changes with file links, commands actually run and PASS/FAIL/BLOCKED results,
and remaining limitations. Separate implementation from verification and deployment.
Do not invent sources, results, or API behavior, present assumptions as facts, reuse
old results as fresh evidence, or claim completion when required checks are blocked.
Tooling tests do not prove agent judgment; instructions reduce unsupported claims
but cannot guarantee zero errors. Explain process only when needed or requested.

Follow scope rules for consequential operations. Do not commit, push, deploy,
mutate persistent databases, or spawn subagents without applicable authorization.
