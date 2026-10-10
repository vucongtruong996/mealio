# Category API — Real HTTP Verification

## Test environment

`CategoryApiIT` starts the actual Spring Boot application on a random local HTTP port with an isolated PostgreSQL 17 Testcontainers database. Requests use `TestRestTemplate`. No use cases, repositories, or audit providers are mocked or replaced. There is no enclosing test transaction; assertions read committed database state.

Each case clears only its container's Category table. It does not connect to the configured local development database.

## Scenarios

| Scenario | Expected behavior |
|---|---|
| Create | `201`, relative `Location`, normalized name/description, `isActive`, no audit fields in response |
| Detail | `200`, same category representation |
| Update | `200`, same ID/active state, changed fields persisted |
| Delete | `204`, empty body, row absent; subsequent detail/delete returns `404` |
| Single duplicate | Whitespace-normalized existing name returns `409` |
| Bulk duplicate in database | `409`, no partial insert |
| Bulk duplicate in request | `409`, no insert |
| Duplicate update | `409`, existing row unchanged |
| Bulk success | `201`, request order retained, both rows committed |
| Listing | Current array contract; pagination/sort query parameters select expected records |
| Invalid bulk | Seven cases: empty array, null element, empty object, malformed JSON, null body, missing description, blank name; `400`, no writes |
| Invalid IDs | GET/PUT/DELETE return `400` |
| Missing IDs | GET/PUT/DELETE return `404`; lookup problem includes `CATEGORY_NOT_FOUND` |
| Production audit wiring | Creation actor/timestamps populated; creation values retained and update fields populated after PUT |

These are twelve JUnit cases, some checking several requests and persistence outcomes.

## Defect found and repaired

The first run passed eleven cases and failed the real-application audit case: `createdBy` was null.

`JpaAuditConfiguration` used `@AutoConfiguration` without auto-configuration registration, so normal application scanning did not enable it. `SystemCurrentActorProvider` was also not registered as a bean. Earlier JPA tests explicitly imported audit configuration and provided test actors, hiding the production wiring gap.

Fix:

- Register audit configuration through `@Configuration(proxyBeanMethods = false)`.
- Register the existing `SystemCurrentActorProvider` through `@Component`.
- Retain the new test with real application beans to prevent regression.

`SYSTEM` remains the existing default actor until authenticated-user identity is implemented. This fix does not backfill historical rows with null audit values.

## Repeat verification

From `mealio-menu-service/`, with Docker running:

```bash
# Run this integration class explicitly through Surefire.
./mvnw -Dtest=CategoryApiIT test

# Complete regression suite
./mvnw clean verify
```

After the fix, full verification passed in 41 seconds: 75 Surefire and 26 Failsafe cases, zero failures/errors/skips. All twelve real HTTP cases passed. Four pre-existing empty methods remain in the total of 101 reported cases.

## Remaining limits

- Actual simultaneous request races and HTTP translation of commit-time name conflicts remain pending. Sequential `409` responses and database rollback do not establish concurrency-safe HTTP errors.
- Schema alignment and migrating from Hibernate automatic updates remain pending.
- No load, latency, authorization, or deployment-environment tests are claimed.
- List pagination metadata is still not returned; this test checks the existing array behavior.

## Source

- [Real HTTP integration tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/presentation/controller/CategoryApiIT.java)
- [Audit configuration](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/infrastructure/audit/JpaAuditConfiguration.java)
- [Default actor provider](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/infrastructure/audit/SystemCurrentActorProvider.java)
