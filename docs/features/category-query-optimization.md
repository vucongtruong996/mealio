# Category Query Optimization — Stage 1

## Scope

Reduce bulk conflict-check round trips and align single-create normalization. Endpoint paths, success bodies, status conventions, batch limits, case sensitivity, and description requirements remain unchanged.

## Before and after

| Operation | Before | After |
|---|---|---|
| Bulk name precheck | Up to 100 individual existence queries | One scalar name projection query for the batch |
| Empty repository name lookup | Not available | Empty set without SQL |
| Single-create conflict check | Raw request name | Validated, normalized domain name |
| Single-create transaction | Persistence save only | Application operation is transactional |

The database query selects only existing names, not complete entities:

```sql
SELECT name FROM categories WHERE name IN (...)
```

`CategoryRepository.findExistingNames(Collection<String>)` expresses the operation without exposing JPA entities. The adapter delegates to a JPQL projection on `CategoryEntity`. The actual SQL schema follows the existing entity mapping; schema alignment is not silently changed here.

## Application behavior

Bulk validates every category, rejects request duplicates, calls `findExistingNames` once, then checks the returned set in request order. If several names conflict, the first conflicting name in the request is reported deterministically. No per-category `existsByName` calls remain in bulk.

Single create constructs the Category before its repository precheck. Thus `" Breakfast "` is checked as `"Breakfast"`, and invalid domain input is rejected before querying. A Spring application transaction wraps lookup and save; it does not eliminate the concurrent uniqueness race.

## Verification strategy

- Unit tests assert normalized arguments, one bulk lookup for 100 items, no individual existence calls, no saves on conflict, and deterministic conflict selection.
- PostgreSQL tests enable Hibernate statistics only in the test context. They assert one prepared statement and zero entity loads for the name projection, and zero statements for empty input.
- Application integration tests run without an outer test transaction to observe committed single creation and bulk rollback.
- Existing stale-precheck rollback coverage now stubs the set-based lookup, retaining real database writes and constraint enforcement.

Reduced query count is verified; a latency percentage or production throughput improvement is not claimed. Insert statement count and JDBC batching are outside this stage.

## Follow-up stages

1. Schema/constraint ownership and recognized concurrent conflict translation.
2. Reviewed pagination response and standardized error contracts.
3. Measured persistence tuning, such as insert batching and deletion queries.

## Repeat verification

From `mealio-menu-service/`:

```bash
./mvnw -Dtest=BulkCreateCategoryUseCaseTest,CreateCategoryUseCaseTest test
./mvnw clean verify
```

## TDD results

1. Extend use-case tests before implementation: six of fourteen fail against the old behavior.
2. Implement one bulk lookup and normalized single creation: all fourteen focused tests pass.
3. Run PostgreSQL tests with a minimal adapter skeleton: four cases fail, confirming the missing projection behavior.
4. Implement the JPQL scalar projection and empty-input shortcut.
5. Full `./mvnw clean verify` passes: 75 Surefire + 14 Failsafe cases, zero failures/errors/skips, in 48 seconds. The count includes four pre-existing empty test methods.

The SQL-count tests confirm exactly one prepared statement with zero entity loads for matching names and no statements for empty input. No production latency benchmark was performed.

## Complete source for review

- [Domain repository contract](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/domain/repository/CategoryRepository.java)
- [JPA name projection](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/infrastructure/persistence/repository/JpaCategoryRepository.java)
- [Persistence adapter](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/infrastructure/persistence/repository/CategoryPersistenceRepository.java)
- [Bulk use case](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/implement/BulkCreateCategoryUseCaseImpl.java)
- [Single-create use case](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/implement/CreateCategoryUseCaseImpl.java)
- [Bulk tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/application/usecase/BulkCreateCategoryUseCaseTest.java)
- [Single-create tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/application/usecase/CreateCategoryUseCaseTest.java)
- [PostgreSQL verification](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/infrastructure/persistence/repository/CategoryBulkIT.java)
