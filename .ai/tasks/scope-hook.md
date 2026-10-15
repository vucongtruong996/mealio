# Harness simplification plan

**SPEC:** [Harness and scope hooks](../../docs/features/engineering-scope-hook.md).
**Authorization:** The user approved the combined five-file simplification proposal
in chat on 2026-10-15. No application changes, executable hook/checker changes,
dependencies, commit, push, or persistent-database operations are included.

## Outcome and file boundary

Make the developer's review simple: one combined proposal, one approval, and one
evidence-based delivery. Preserve review/strict enforcement and existing repository
work. Reuse this PLAN and the existing SPEC rather than creating more records.

| File | Responsibility |
| --- | --- |
| `.agents/skills/harness/SKILL.md` | Define propose → approve → deliver, concise proposal/delivery content, scoped TDD, self-review, and evidence discipline. |
| `.ai/rules/scope.md` | Own approval semantics: SPEC-only versus combined approval, continuation, and material amendments. Preserve enforcement mechanics. |
| `docs/features/engineering-scope-hook.md` | Maintain observable instruction-level criteria HC09–HC12 alongside existing hook criteria. |
| `.ai/tasks/scope-hook.md` | Keep this bounded implementation and verification plan in place. |
| `docs/README.md` | Provide a short example and explain the review/approval handoff. |

## Implementation and verification

1. Inspect current instructions and record file hashes outside the repository to
   distinguish this change from existing uncommitted harness work.
2. Simplify the skill, clarify approval in the rules, and align the SPEC/index.
   Preserve context discovery, existing edits, meaningful TDD, and consequential
   operation authorization. Do not change hook registration or strict behavior.
3. Review the scenarios below against the final instructions. This is manual
   contract review, not a behavioral agent evaluation. Documentation-only changes
   use consistency and document checks; artificial Red tests are not appropriate.
4. Check links, frontmatter, whitespace, and the exact five-file change boundary.
   Use the bundled skill validator when available; report dependency blockers
   without adding project dependencies. Run the existing tooling suite:

   ```bash
   python3 -B -m unittest discover -s scripts/tests -p 'test_scope_guard.py' -v
   ```

5. Review final files and actual checks against HC08–HC12. Report results and
   limitations in the handoff; do not create a verification diary.

## Manual contract-review scenarios

| Scenario | Expected instruction-level decision | Criteria |
| --- | --- | --- |
| User requests a preview | Inspect and present the proposal; no writes, tests, or implementation. | HC10 |
| User approves the SPEC only, with an older PLAN present | Do not start implementation; present the combined proposal for approval. | HC10 |
| User approves the combined proposal; an in-scope test fails | Diagnose/fix and rerun within scope without requesting ordinary approval again. | HC10 |
| Required verification is unavailable | Report BLOCKED and unverified acceptance criteria; do not claim complete verification or invent results. | HC11 |
| Source behavior has not been checked | Inspect it or identify uncertainty; do not present assumptions as facts. | HC09, HC11 |
| A discovered fix needs a materially different file/behavior boundary | Request approval of the amendment; continue independent authorized work. | HC12 |
| User asks a follow-up or status question | Answer and continue the existing task without new task documents. | HC08–HC10 |

## Limits and retained tooling

Review mode supplies instructions and leaves native tool permissions unchanged;
it does not authenticate chat approval or mechanically enforce these workflow
criteria. Strict mode remains available with its human receipt requirements.
The existing harness scope manifest is historical, not new approval.

Executable subprocess checks can verify hook behavior, not agent judgment or future
compliance. Live client trust/event delivery remains unverified. Evidence rules
reduce unsupported claims but cannot guarantee zero hallucinations. Independent
agent evaluation is not included or authorized in this change.
