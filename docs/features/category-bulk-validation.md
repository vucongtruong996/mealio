# Category Bulk Validation

## Scope and contract

`POST /api/v1/categories/bulk` retains its JSON array request and response. Accept 1–100 categories, each with a nonblank name (maximum 100 characters) and nonblank description (maximum 500 characters), matching the current domain requirements. Null elements are invalid.

Normalize through the domain before comparing names. Comparison remains case-sensitive. Reject duplicate normalized names within a batch and names already in the database with `409`. Validate every category and check every conflict before calling `saveAll`. Preserve input ordering in successful responses and save all or none.

Invalid HTTP input returns `400` before calling the use case. Direct application callers also receive explicit rejection for null/empty/oversized batches and null commands. No records are silently skipped.

## TDD plan

1. Unit tests for success, ordering, duplicate request names, existing names, normalization, invalid fields, and invalid batches.
2. HTTP tests for `201`, validation, malformed JSON, and `409`.
3. PostgreSQL application integration tests with an actual transactional bean to verify persistence and no partial inserts.
4. Full Maven verification and maintenance documentation update.

## Boundaries

This increment implements application prechecks and HTTP validation. It preserves the database constraint as the final concurrency safeguard. Recognized constraint translation for concurrent requests, explicit migration ownership, and schema alignment remain pending; this feature does not claim concurrent conflicts always return `409`.

Single-create descriptions will also be validated as nonblank at the HTTP boundary because bulk and single create share `CreateCategoryRequest`. This aligns with the existing domain invariant rather than making descriptions optional.

## Implementation and verification

The initial verification counts below are historical. [Stage 1 query optimization](category-query-optimization.md) replaces the per-name database prechecks with one name projection and expands the tests.

- Six new unit tests cover normalized ordered success, request duplicates, existing names, whole-batch domain validation, invalid batch shapes, and case-sensitive names. The first run had four expected failures; the other two behaviors already worked.
- Twelve new HTTP cases cover ordered `201`, empty/null/invalid batches, malformed JSON, oversized batches, `409`, and nonblank descriptions on single create. Before implementation, four assertions and three request-processing cases failed; after implementation all pass.
- `@Size` on the request collection activates Spring MVC method validation; container-element `@NotNull` and `@Valid` validate each item before controller mapping. Do not remove the collection constraint without verifying element validation again.
- Four PostgreSQL tests verify committed success, request duplicates, existing names, and actual rollback when the database rejects a duplicate after a simulated stale precheck. These run without an enclosing test transaction.
- `./mvnw clean verify` passed: 71 Surefire and 10 Failsafe cases, zero failures/errors/skips. Four pre-existing empty methods remain in that reported count.

## Source and test locations

- [Bulk implementation](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/implement/BulkCreateCategoryUseCaseImpl.java)
- [Controller validation](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/presentation/controller/CategoryController.java)
- [Create request](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/presentation/request/CreateCategoryRequest.java)
- [Unit tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/application/usecase/BulkCreateCategoryUseCaseTest.java)
- [HTTP tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/presentation/controller/CategoryControllerTest.java)
- [PostgreSQL tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/infrastructure/persistence/repository/CategoryBulkIT.java)
