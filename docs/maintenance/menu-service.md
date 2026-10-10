# Menu Service Maintenance Guide

**Last reviewed:** 2026-10-10
**Scope:** Current Category implementation in `mealio-menu-service`
**Purpose:** Locate changes, preserve behavior, and verify modifications safely.

This guide describes current behavior, not all planned platform capabilities. Consult the [engineering standard](../architecture/00-project-engineering-standard.md) for concepts and the feature specifications for detailed contracts.

Start with the [complete Category specification and roadmap](../features/category-api-spec.md) for the consolidated endpoint contracts and feature status. Use this guide to locate and verify modifications.

## 1. Current API behavior

All paths below start with `/api/v1/categories`.

| Method/path | Current behavior | Maintenance notes |
|---|---|---|
| `POST /` | Create one category; `201` and `Location` | Duplicate precheck exists |
| `POST /bulk` | Create 1–100 categories; `201` and ordered array | Reject normalized request duplicates and existing names before saving |
| `GET /` | List page content; `200` and array | Pagination metadata is currently discarded |
| `GET /{id}` | Get category, including inactive; `200` or `404` | Invalid UUID produces `400` |
| `PUT /{id}` | Replace name/description; `200`, `404`, or prechecked `409` | Both fields required; preserves ID and active state |
| `DELETE /{id}` | Physically remove category; `204` or `404` | Repeated delete returns `404` |

Category responses contain `id`, `name`, `description`, and **`isActive`**. Do not rename `isActive` to `active` without a deliberate API compatibility decision. Audit fields are not part of these responses.

Create, bulk create, and update require nonblank name/description with request limits of 100/500 characters. Domain methods trim whitespace. Bulk requests reject empty/oversized arrays and null elements before invoking the use case.

The `404` handler returns `ProblemDetail` with code `CATEGORY_NOT_FOUND`. The existing duplicate handler also uses `ProblemDetail`, but does not currently include a `code` property. Do not assume every error has the same shape.

## 2. Where to change code

Java paths are relative to `mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/`. Test packages mirror the main packages under `src/test/java/`.

| Change | Main location | Verification |
|---|---|---|
| Endpoint, status, header | `presentation/controller/CategoryController.java` | Controller tests |
| Update HTTP fields/constraints | `presentation/request/UpdateCategoryRequest.java` | Mutation controller tests |
| Create HTTP fields/constraints | `presentation/request/CreateCategoryRequest.java` | Create controller tests |
| Request-to-command mapping | `presentation/mapper/CategoryPresentationMapper.java` | Mapping and controller tests |
| Use-case inputs | `application/command/` | Use-case and controller tests |
| Lookup/update/delete orchestration | `application/usecase/implement/` | Corresponding use-case tests |
| Business validation or state transitions | `domain/model/Category.java` | Domain tests plus affected use cases |
| Persistence contract | `domain/repository/CategoryRepository.java` | All callers and adapter tests |
| Queries/entity updates | `infrastructure/persistence/repository/` | PostgreSQL integration tests |
| Bulk name conflicts | `CategoryRepository.findExistingNames` and its JPA projection | Bulk unit tests and SQL-count integration test |
| Domain/entity conversion | `infrastructure/persistence/mapper/CategoryPersistenceMapper.java` | Mapper and persistence tests |
| Table/column mapping | `infrastructure/persistence/entity/CategoryEntity.java` | Migration and PostgreSQL tests |
| Audit identity/timestamps | `infrastructure/audit/` | Audit and mutation integration tests |
| Exception-to-HTTP mapping | `presentation/advice/GlobalExceptionHandler.java` | Controller error tests |
| Database evolution | `src/main/resources/db/migration/` | Fresh initialization and upgrade verification |

Use-case interfaces live under `application/usecase/category/`. Application and presentation outputs live in `application/result/` and `presentation/response/`.

## 3. Execution and transaction boundaries

```text
Request → Controller → Command/use-case interface
        → Application implementation → Domain methods
        → Repository interface → Persistence adapter → JPA/PostgreSQL
        → Application result → Response DTO
```

- Keep controllers thin; they must not call JPA repositories.
- Keep JPA entities inside infrastructure.
- Update/delete use cases are Spring-managed transactional beans. Lookup is read-only transactional.
- Same-object method calls generally bypass Spring transaction proxy interception.
- `save()` does not guarantee SQL has executed. Constraint errors can appear during flush or commit.
- `CategoryPersistenceRepository.save()` loads an existing entity before applying business-field changes. Preserve this behavior: merging a fresh entity previously cleared creation audit fields in memory.
- Loading a domain object does not make it a JPA-managed entity. The adapter performs the persistence conversion/update.
- Bulk checks stored names with one projection query after in-memory validation. Preserve request-order conflict reporting and avoid reintroducing one query per category.
- Single create normalizes and validates the domain object before its name query; the application operation is transactional.
- Audit configuration is a scanned `@Configuration`; the default `SYSTEM` actor provider is a component. Verify normal startup with `CategoryApiIT`, not only explicit test imports of auditing beans.

## 4. Procedure for a modification

1. Inspect `git status`, the current branch, relevant implementation, and tests. Preserve unrelated work.
2. Write the intended behavior and acceptance examples in a feature document. Identify compatibility, schema, and transaction impacts.
3. Add a regression test or new behavior test. Run it and confirm the failure represents the intended gap.
4. Implement the smallest complete change at the correct layer.
5. Run focused tests. If a controller dependency changes, update its slice-test mocks.
6. For persistence changes, test with PostgreSQL and explicitly flush/clear when verifying database state.
7. Run `./mvnw clean verify`, inspect failures/skips, and review the diff.
8. Update the affected specification, this guide when behavior changes, and the change record below.
9. Obtain review before committing or pushing.

For a bug fix, prefer a test that fails against the old behavior. A passing mock test alone cannot establish database rollback or concurrent correctness.

## 5. Common change recipes

### Add or change a Category field

Decide whether it is writable, readable, optional, and persisted. Update the applicable request/command, domain behavior, entity, all boundary mappers, result/response, tests, and specification. Add a migration for persisted fields, including backfill/default behavior for existing rows. Do not add a public entity setter solely to satisfy a test.

### Change name uniqueness

Define normalization and case sensitivity first. Align single create, bulk create, update, and database constraints. Update must exclude the current category from conflicts. Test whitespace, unchanged names, different owners, and concurrent attempts. Prechecks improve messages but cannot replace database enforcement.

### Make descriptions optional in a future change

Descriptions are currently required across create/update. To change this, align request constraints, domain normalization, nullability annotations, persistence mapping, response semantics, and existing rows. Decide whether blank becomes null. The current mapper substitutes an empty string for null, which the domain rejects; address the entire path together.

### Change deletion policy

Physical delete is current behavior. Soft deletion/deactivation changes queries, repeat-delete behavior, uniqueness, and visibility. Before adding menu items, define foreign keys and whether referenced categories can be removed. Specify migration and API compatibility effects before implementation.

### Change auditing or schema

Add a migration rather than modifying a migration already applied to a shared environment. Verify Flyway and Hibernate use the intended schema. Preserve creation fields on update, obtain actors from trusted server-side context, and test timestamp behavior. Hibernate `ddl-auto: update` is still configured; migration/schema alignment and moving to validation remain pending.

## 6. Test commands and reports

Run from `mealio-menu-service/`, using Java 17. Testcontainers tests require Docker.

```bash
# Domain and lookup
./mvnw -Dtest=CategoryTest,GetCategoryByIdUseCaseTest test

# Update/delete use cases and HTTP
./mvnw -Dtest=UpdateCategoryUseCaseTest,DeleteCategoryUseCaseTest,CategoryMutationControllerTest test

# Both controller suites
./mvnw -Dtest=CategoryControllerTest,CategoryMutationControllerTest test

# Full build, Surefire, and Failsafe
./mvnw clean verify
```

Reports are generated under `target/surefire-reports/` and `target/failsafe-reports/`; `clean` removes previous reports. New integration tests should use `*IT` naming for Failsafe. Existing `CategoryAuditingIntegrationTest` runs under Surefire with embedded JPA testing.

If Mockito cannot initialize its agent, inspect the JDK and execution restrictions before changing application code. If a container fails, inspect Docker and Testcontainers logs before attributing it to a business-rule failure.

### Verification snapshot: 2026-10-10

| Suite | Reported cases |
|---|---:|
| `CreateCategoryUseCaseTest` | 6 |
| `GetCategoryByIdUseCaseTest` | 3 |
| `BulkCreateCategoryUseCaseTest` | 8 |
| `UpdateCategoryUseCaseTest` | 6 |
| `DeleteCategoryUseCaseTest` | 3 |
| `CategoryTest` | 3 |
| `CategoryPersistenceMapperTest` | 4 |
| `CategoryPresentationMapperTest` | 2 |
| `CategoryAuditingIntegrationTest` | 2 |
| `CategoryControllerTest` | 24 |
| `CategoryMutationControllerTest` | 14 |
| `CategoryPersistenceRepositoryIT` | 2 |
| `CategoryMutationIT` | 4 |
| `CategoryBulkIT` | 8 |
| `CategoryApiIT` | 12 |
| **Total** | **101** |

Last full run after real HTTP verification and the audit-wiring fix: 75 Surefire + 26 Failsafe cases; no failures, errors, or skips. This is a dated snapshot, not a guarantee for future changes.

Four reported cases are empty: both `CategoryPresentationMapperTest` methods and the pagination/sorting methods in `CategoryControllerTest`. `CreateCategoryUseCaseTest.shouldInitializeAuditFields` only checks a save interaction because its audit assertions are commented out. Actual audit checks exist in the JPA audit and PostgreSQL mutation tests.

MockMvc suites mock use cases. PostgreSQL mutation tests run in test-managed transactions. Bulk integration tests instead disable the test transaction and inspect state after actual application commit/rollback; one stubs a stale name precheck while retaining real SQL. `CategoryApiIT` starts the actual server and uses HTTP with real application beans and PostgreSQL. An actual multi-threaded race remains untested.

## 7. Known work to track

| Gap | Next action |
|---|---|
| Concurrent bulk duplicate error translation | Recognize the name constraint without mislabeling unrelated failures |
| Optional descriptions | Required behavior is now consistent; optionality needs an explicit future decision |
| Pagination metadata discarded | Agree on public response and implement real tests |
| Four empty tests | Replace with assertions or remove misleading placeholders |
| Misnamed audit unit test | Rename/remove redundant test or add appropriate assertions |
| Blank-description exception names the category name | Define a suitable validation exception |
| Concurrent uniqueness failures | Translate only recognized constraints and test racing writes |
| Concurrent update/delete | Define lost-update protection and deletion race semantics |
| Flyway/Hibernate alignment | Explicit schema ownership, constraints, and validation |
| API-standard envelope differs from current errors | Record and apply a compatibility decision |
| Administrative authorization | Define access policy before external exposure |
| Historical null audit fields | Audit wiring is fixed for new writes; define a data-repair policy for any older rows |

## 8. Change record and reusable template

| Date | Change | Evidence/details |
|---|---|---|
| 2026-10-10 | Get-by-ID implemented | [Lookup specification and TDD record](../features/category-get-by-id.md) |
| 2026-10-10 | Update/delete implemented; creation audit preservation fixed | [Mutation specification and TDD record](../features/category-update-delete.md) |
| 2026-10-10 | Full suite verified; maintenance baseline documented | 59 reported passing cases, with coverage limitations above |
| 2026-10-10 | Bulk validation and duplicate prechecks implemented | [Bulk specification](../features/category-bulk-validation.md); includes post-transaction PostgreSQL checks |
| 2026-10-10 | Stage 1 query optimization | [Optimization specification](../features/category-query-optimization.md); one bulk name query and normalized single create |
| 2026-10-10 | Real HTTP verification; audit beans registered for normal startup | [Verification record](../features/category-api-verification.md); all 101 reported cases pass |

For a future change, create `docs/features/<feature-name>.md` using:

```markdown
# Feature or Change Title

## Problem and scope
## Current behavior and desired behavior
## API and compatibility impact
## Domain rules and acceptance examples
## Code locations and database migrations
## Transactions, concurrency, and failure behavior
## TDD steps and verification results
## Known limits and follow-up work
```

Record actual outcomes, not only intended behavior. Link the feature record here after implementation; keep old test counts clearly dated.
