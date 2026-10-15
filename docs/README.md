# Mealio Documentation

## AI engineering workflow

- [Harness skill](../.agents/skills/harness/SKILL.md): propose → approve → deliver, with one reviewable SPEC/PLAN, scoped TDD, and actual verification evidence.
- [Scope and approval rules](../.ai/rules/scope.md): review and strict approval channels, protected operations, and enforcement limitations.
- [Harness and scope hook specification](features/engineering-scope-hook.md): observable tooling behavior and boundaries.

Example prompt: `$harness propose <feature>. Preview first.`
Review the outcome, evidence/assumptions, acceptance examples, affected files,
test commands, and unresolved decisions together. Then approve the combined
SPEC/PLAN to authorize implementation. SPEC-only approval does not start coding.
The agent continues in-scope tests and fixes, reviews its diff, and delivers actual
results and limitations. Scope changes and consequential operations need separate
authorization. Small tasks stay in chat; complex tasks reuse one SPEC and one PLAN.
Blocked checks are reported honestly; instructions cannot guarantee zero errors.

## Maintaining the menu service

- [Complete Category specification and roadmap](features/category-api-spec.md): all endpoints, shared rules, feature status, acceptance evidence, and pending work. Start here for Category changes.
- [Menu service maintenance guide](maintenance/menu-service.md): current contracts, code map, modification recipes, tests, known gaps, and change record.
- [Get Category by ID](features/category-get-by-id.md): lookup contract and TDD walkthrough.
- [Category update and delete](features/category-update-delete.md): mutation contracts, auditing fix, TDD evidence, and complete source links.
- [Category bulk validation](features/category-bulk-validation.md): batch limits, duplicate checks, request validation, and transaction verification.
- [Category query optimization](features/category-query-optimization.md): one bulk conflict query, SQL-count verification, and normalized single creation.
- [Real Category API verification](features/category-api-verification.md): actual HTTP-to-PostgreSQL checks and the application audit-wiring fix.

## Learning and architecture

- [Project engineering standard — draft](architecture/00-project-engineering-standard.md)
- [System overview](architecture/01-system-overview.md)
- [API standard](architecture/02-api-standard.md)
- [Backend standard](architecture/03-backend-standard.md)
- [AI architecture](architecture/04-ai-agent-architecture.md)
- [Database design](architecture/05-database-design.md)
- [gRPC contracts](architecture/06-grpc-contract.md)
- [Event design](architecture/07-event-driven-design.md)
- [Deployment architecture](architecture/08-deployment-architecture.md)

Architecture documents include future goals. Use the maintenance guide and feature records to identify current implementation and documented gaps; resolve discrepancies explicitly when changing a contract.
