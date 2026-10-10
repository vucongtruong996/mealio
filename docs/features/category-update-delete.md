# Category Update and Delete — Specification and TDD Guide

## Update contract

`PUT /api/v1/categories/{id}` replaces the editable name and description of an existing category. It does not create a missing category or change its ID or active state.

```json
{
  "name": "Lunch",
  "description": "Afternoon meals"
}
```

- Both fields are required and nonblank, consistent with current domain invariants.
- Request limits are 100 characters for name and 500 for description.
- Domain methods trim surrounding whitespace.
- Names are case-sensitive. Another category owning the normalized name causes `409`; retaining the current category's name is valid.
- Clients cannot modify identity, active state, or audit fields through this operation.
- Success returns `200` with `id`, `name`, `description`, and `isActive`.
- Missing category returns the existing `404` ProblemDetail contract.
- Invalid UUID, malformed JSON, or invalid request fields return `400` before the use case executes.

A dedicated `UpdateCategoryRequest` avoids changing the existing create contract. Optional descriptions across create/update remain a separate project decision.

## Delete contract

`DELETE /api/v1/categories/{id}` physically removes the category, matching the existing repository operation. This is not deactivation or soft deletion.

- Existing category: `204` with an empty body.
- Missing category, including a repeated delete: `404`.
- Invalid UUID: `400` before invoking the use case.

Repeated delete leaves the same final state even though its response changes. Future menu-item relationships need an explicit deletion/referential-integrity policy before this behavior is extended to related data.

## Architecture

```text
Controller → Use-case interface → Transactional implementation
           → Domain behavior → Repository adapter → JPA
```

Update uses `rename` and `changeDescription`, checks normalized name ownership, saves, and maps the result. Delete loads the category to establish existence before deleting by ID. Both use cases define application transaction boundaries.

The persistence adapter now loads an existing entity before changing business fields. Mapping a fresh entity on update had cleared creation audit fields in the persistence context. Updating the managed entity preserves those fields and allows JPA auditing to maintain update fields on flush.

## TDD record

1. Add compilable use-case interfaces, command, and empty implementation skeletons.
2. Write nine use-case tests; run them and observe nine expected failures.
3. Implement update/delete orchestration; all nine tests pass.
4. Write fourteen HTTP test cases; seven fail against placeholder behavior, while existing MVC parsing/validation already handles the others.
5. Add the update request/mapper, connect both endpoints, and update existing slice-test mocks for the new dependencies.
6. Write four PostgreSQL integration tests. The audit-preservation test fails because creation time becomes null in the managed entity.
7. Fix existing-entity persistence and run full verification.
8. Full `./mvnw clean verify` succeeds: 53 Surefire tests and 6 Failsafe tests, with zero failures, errors, or skips. This includes 27 new test cases and four pre-existing empty test methods: two controller pagination/sorting methods and two presentation-mapper methods.

See the [maintenance guide](../maintenance/menu-service.md) for the current code map, modification checklist, and known coverage gaps.

## Coverage

| Level | What is verified |
|---|---|
| Update unit tests | Normalization, preserved identity/state, unchanged name, conflicting owner, missing record, invalid input, null arguments |
| Delete unit tests | Existing deletion, missing record, null ID |
| HTTP tests | Delegation, response JSON, blank/oversized/missing fields, malformed JSON, UUID conversion, `404`, `409`, empty `204` |
| PostgreSQL tests | Persisted update, creation audit preservation, update audit population, rejected duplicate leaves original fields, physical deletion, repeated deletion, missing update |

Integration tests run in rollback-managed test transactions. They verify SQL behavior and managed entities, not end-to-end HTTP commit semantics or concurrent races.

## Known limits

- Duplicate prechecks do not close the concurrent-write race. The database constraint remains essential; recognized constraint translation for racing writes and migration ownership are separate pending work.
- Concurrent update/delete protection and optimistic locking are not introduced here.
- Existing Hibernate/Flyway schema-alignment work remains pending.
- Authorization follows the existing service setup and is not added by this feature.

## Verification

From `mealio-menu-service/`:

```bash
./mvnw -Dtest=UpdateCategoryUseCaseTest,DeleteCategoryUseCaseTest,CategoryMutationControllerTest test
./mvnw clean verify
```

## Complete source for review

- [Update command](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/command/UpdateCategoryCommand.java)
- [Update interface](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/category/UpdateCategoryUseCase.java)
- [Update implementation](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/implement/UpdateCategoryUseCaseImpl.java)
- [Delete interface](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/category/DeleteCategoryUseCase.java)
- [Delete implementation](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/implement/DeleteCategoryUseCaseImpl.java)
- [Update request](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/presentation/request/UpdateCategoryRequest.java)
- [Controller](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/presentation/controller/CategoryController.java)
- [Presentation mapper](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/presentation/mapper/CategoryPresentationMapper.java)
- [Persistence adapter](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/infrastructure/persistence/repository/CategoryPersistenceRepository.java)
- [Update tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/application/usecase/UpdateCategoryUseCaseTest.java)
- [Delete tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/application/usecase/DeleteCategoryUseCaseTest.java)
- [HTTP tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/presentation/controller/CategoryMutationControllerTest.java)
- [Existing controller test wiring](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/presentation/controller/CategoryControllerTest.java)
- [PostgreSQL tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/infrastructure/persistence/repository/CategoryMutationIT.java)
