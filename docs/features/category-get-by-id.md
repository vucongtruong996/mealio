# Get Category by ID — Contract and TDD Guide

## Contract

`GET /api/v1/categories/{id}` retrieves a category by UUID, including an inactive category. This operation performs no writes and accepts no request body.

| Condition | Result |
|---|---|
| Category exists | `200`, JSON `CategoryResponse` |
| Valid UUID is absent | `404`, `application/problem+json` |
| Invalid UUID syntax | `400`, handled by Spring MVC |

Success fields are `id`, `name`, `description`, and **`isActive`**, matching the existing response record. Audit fields are excluded.

A missing-category problem contains status `404`, title `Category Not Found`, type `errors/category-not-found`, code `CATEGORY_NOT_FOUND`, and the existing exception message. Broader API-envelope changes are outside this change.

## Execution flow

```text
Spring MVC converts path to UUID
 → CategoryController
 → GetCategoryByIdUseCase.execute(id)
 → CategoryRepository.findById(id)
 → CategoryApplicationMapper.toResult(category)
 → CategoryPresentationMapper.toCategoryResponse(result)
 → JSON response
```

The existing persistence adapter implements the repository lookup. No new table or migration is needed.

## TDD sequence performed

1. Write use-case tests before implementation: found, missing, and null ID.
2. Run tests: compilation fails because the implementation does not exist.
3. Add an interface and minimal implementation skeleton. Run again outside the sandbox after Mockito agent attachment fails inside it: all three behavior tests fail as expected.
4. Implement null validation, repository lookup, exception on absence, and result mapping. All three tests pass.
5. Write HTTP tests before controller implementation. Add missing bulk-use-case mock to repair the existing slice-test wiring.
6. Run controller tests: success has no body, and missing lookup incorrectly returns `200`; both tests fail. Invalid UUID already returns `400` through Spring MVC.
7. Connect the controller and add the missing-category exception handler.
8. Correct the new success assertion to use the existing `isActive` JSON property rather than assuming `active`.
9. Run focused tests: 15 pass, with no failures or errors. This includes two pre-existing empty controller test methods, which provide no behavioral coverage.
10. Run `./mvnw clean verify`: four existing single-create tests fail because `@InjectMocks` targets an interface. Change that test field to `CreateCategoryUseCaseImpl`; production single-create behavior is unchanged.
11. Repeat full verification: build succeeds, with 30 Surefire tests and 2 Failsafe tests passing, zero failures/errors/skips. The repository integration tests connect to PostgreSQL 17 through Testcontainers. These existing tests do not constitute an end-to-end test of the new endpoint.

The historical full-run count includes four empty methods: two controller pagination/sorting tests and two presentation-mapper tests. See the [maintenance guide](../maintenance/menu-service.md) for the current verification snapshot and coverage gaps.

## What the new tests prove

| Test | Evidence |
|---|---|
| Found use-case result | Repository domain object is mapped to the application result |
| Missing category | Specific exception is raised; mapper is not called |
| Null ID | Rejected before repository or mapper interaction |
| HTTP success | UUID delegation, JSON fields, and audit-field exclusion |
| HTTP missing | Exception is translated to the documented `404` problem |
| Invalid path | MVC rejects the UUID before invoking the use case |

## Concepts

- Constructor injection gives the use case its repository and mapper.
- `Optional` distinguishes an absent record from a successful lookup.
- The application throws a business exception; presentation chooses its HTTP representation.
- `@Transactional(readOnly = true)` expresses read-only transaction intent when invoked through Spring's proxy. Mockito unit tests do not prove transaction proxy behavior.
- MockMvc verifies the HTTP boundary with a mocked use case. These tests do not prove live PostgreSQL access or complete application startup.

## Repeat the focused verification

From `mealio-menu-service/`:

```bash
./mvnw -Dtest=GetCategoryByIdUseCaseTest,CategoryControllerTest test
```

Run `./mvnw clean verify` for the broader baseline. Any failures outside the focused tests must be reported separately rather than treating the full service as verified.

## Source files for review

- [Use-case interface](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/category/GetCategoryByIdUseCase.java)
- [Use-case implementation](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/application/usecase/implement/GetCategoryByIdUseCaseImpl.java)
- [Controller](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/presentation/controller/CategoryController.java)
- [Exception handler](../../mealio-menu-service/src/main/java/com/mealio/mealio_menu_service/presentation/advice/GlobalExceptionHandler.java)
- [Use-case tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/application/usecase/GetCategoryByIdUseCaseTest.java)
- [Controller tests](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/presentation/controller/CategoryControllerTest.java)
- [Existing create test setup correction](../../mealio-menu-service/src/test/java/com/mealio/mealio_menu_service/application/usecase/CreateCategoryUseCaseTest.java)
