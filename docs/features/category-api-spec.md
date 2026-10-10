# Category API — Feature Specification and Roadmap

**Last reviewed:** 2026-10-10

**Status:** Current implementation documented; remaining work listed separately.

**Scope:** Category functionality in `mealio-menu-service`. This is the starting point for Category changes. The linked TDD records describe implementation history; the [maintenance guide](../maintenance/menu-service.md) maps behavior to source files.

## 1. Purpose and boundaries

Categories organize the shop's menu, for example Breakfast, Coffee, and Desserts. The menu service owns their identity, name, description, and active state.

Menu-item relationships, category ordering, image assets, and activation APIs are not implemented by the current Category endpoints. Inventory quantities and order lifecycle belong to other services.

## 2. Feature inventory

| Feature | Current status | Evidence or remaining work |
|---|---|---|
| Single create | Implemented | Normalized conflict check; transactional save; real HTTP tests |
| Bulk create | Implemented with known concurrency limitation | Limits, validation, request/database prechecks, one conflict query, committed success and rollback tests |
| Detail lookup | Implemented | `200`, `404`, invalid UUID handling |
| Listing | Partial | Paging/sorting work; response discards metadata; bounds and sort allowlist not explicitly implemented |
| Update | Implemented with known concurrency limitation | Required business fields; preserves ID/state and creation auditing |
| Physical delete | Implemented | Missing/repeated delete gives `404`; relationship policy needed before menu items |
| Auditing | Implemented for new writes | Normal-startup bean registration verified; historical null values are not repaired |
| Errors | Partial | Known `404`/prechecked `409`; validation and concurrent database errors not unified |
| Authorization | Pending | No administrator access policy added |
| OpenAPI | Pending | This Markdown contract is not a machine-readable OpenAPI document |
| Activation/deactivation | Domain methods only | No application/HTTP operation |
| Schema ownership | Pending alignment | Flyway targets `menu_schema`; entity schema is unspecified; Hibernate still updates schema |

## 3. Shared data and validation rules

### Category response

```json
{
  "id": "04e91d77-6dad-4657-b531-27b86bf97e73",
  "name": "Breakfast",
  "description": "Morning meals",
  "isActive": true
}
```

| Field | Rule |
|---|---|
| `id` | Server-generated UUID; preserved during updates |
| `name` | Required, nonblank; maximum 100 characters in HTTP input; trimmed by domain |
| `description` | Required, nonblank; maximum 500 characters in HTTP input; trimmed by domain |
| `isActive` | True on creation; returned to clients; not writable through current request DTOs |

HTTP length constraints evaluate submitted strings before domain trimming. Direct use-case callers are protected by domain validation, which evaluates normalized lengths. These boundaries are not identical for padded inputs near the limit.

Name comparison is case-sensitive: `Coffee` and `coffee` are distinct. Surrounding whitespace does not distinguish names. ID, active state, and audit inputs are not mapped from create/update requests; rejection of unknown JSON fields is not promised.

Audit columns are `created_by`, `created_at`, `updated_by`, and `updated_at`. Current default actor is the server-side `SYSTEM` provider. API responses omit audit data. Authenticated actor propagation remains future work.

## 4. Endpoint contracts

Base path: `/api/v1/categories`. Request bodies use `application/json`.

### 4.1 Create one category

```http
POST /api/v1/categories
```

```json
{
  "name": "Breakfast",
  "description": "Morning meals"
}
```

- Success: `201 Created`, Category response, `Location: /api/v1/categories/{id}`.
- Invalid request: `400`; unsupported media type: `415`.
- Existing normalized name found by precheck: `409`.
- Normalize and validate before querying. Query and save execute within the application transaction.

### 4.2 Create a batch

```http
POST /api/v1/categories/bulk
```

```json
[
  {"name": "Breakfast", "description": "Morning meals"},
  {"name": "Lunch", "description": "Afternoon meals"}
]
```

- Accept 1–100 non-null elements, each satisfying create validation.
- Success: `201`, array of Category responses in request order.
- Empty/oversized/null/invalid input: `400` before use-case execution.
- Duplicate normalized names in the request: `409`.
- Existing database names found by precheck: `409`; report the first conflict in request order.
- Validate all categories and reject request duplicates before querying.
- Fetch existing names in one scalar projection query, then save all or none.
- Do not silently skip records. This endpoint does not return per-element partial success.

The unique constraint remains essential. A database conflict missed by a stale precheck rolls back the batch, but its HTTP translation to `409` remains pending.

### 4.3 Get one category

```http
GET /api/v1/categories/{id}
```

- Existing UUID: `200`, Category response, including an inactive category.
- Missing UUID: `404`.
- Invalid UUID syntax: `400` before use-case execution.

### 4.4 List categories

```http
GET /api/v1/categories?page=0&size=20&sort=name,asc
```

Current response is an array:

```json
[
  {
    "id": "04e91d77-6dad-4657-b531-27b86bf97e73",
    "name": "Breakfast",
    "description": "Morning meals",
    "isActive": true
  }
]
```

- Success: `200`; an empty page returns `[]`.
- Spring Data `Pageable` handles paging/sorting parameters.
- No active-state filter is applied.
- Page totals, explicit invalid-input policy, sort allowlist, and deterministic default ordering are not part of the implemented contract yet.
- The existing `PageResponse` record is not used by this endpoint. Returning it requires an explicit compatibility decision.

### 4.5 Update a category

```http
PUT /api/v1/categories/{id}
```

Use the same two business fields as create; both are required. This is not a partial PATCH or an upsert.

- Success: `200`, updated Category response.
- Preserve ID, active state, and creation audit fields.
- Retaining the category's own normalized name is valid.
- Another category owning that name: prechecked `409`.
- Missing category: `404`; invalid request/UUID: `400`.
- Both active and inactive categories can have their editable fields updated.

### 4.6 Delete a category

```http
DELETE /api/v1/categories/{id}
```

- Existing category: physical deletion, `204`, no response body.
- Missing category, including repeated delete: `404`.
- Invalid UUID: `400`.
- No cascade or menu-item relationship behavior is promised. Define that policy before adding related data.

Repeated DELETE is idempotent in final resource state even though the response changes from `204` to `404`.

## 5. Errors and remaining consistency work

Missing category uses `application/problem+json` with fields including:

```json
{
  "type": "errors/category-not-found",
  "title": "Category Not Found",
  "status": 404,
  "detail": "Category '04e91d77-6dad-4657-b531-27b86bf97e73' was not found.",
  "code": "CATEGORY_NOT_FOUND"
}
```

Duplicate prechecks produce a ProblemDetail with type `errors/category-already-exists`, title `Category Already Exists`, status `409`, and detail such as `Category Breakfast already exists.` This handler currently omits a machine-readable `code`.

Spring handles malformed JSON, UUID conversion, and validation failures. Their response fields are not unified by a project-wide handler. Do not assume every failure has the `404` shape. Unexpected integrity errors must not be relabeled as category-name conflicts.

## 6. Implementation map

| Responsibility | Main classes |
|---|---|
| HTTP boundary | `CategoryController`, `CreateCategoryRequest`, `UpdateCategoryRequest` |
| Transport mapping | `CategoryPresentationMapper`, `CategoryResponse` |
| Commands/results | `CreateCategoryCommand`, `UpdateCategoryCommand`, `CategoryResult` |
| Operations | Create, BulkCreate, GetCategoryById, ListCategories, Update, Delete use-case interfaces and implementations |
| Business invariants | `Category`, domain exceptions |
| Persistence contracts | `CategoryRepository`, including `findExistingNames` |
| Persistence implementation | `CategoryPersistenceRepository`, `JpaCategoryRepository`, `CategoryPersistenceMapper`, `CategoryEntity` |
| Auditing | `JpaAuditConfiguration`, `AuditorAwareImpl`, `SystemCurrentActorProvider`, `AuditableEntity` |
| Error translation | `GlobalExceptionHandler` |

See the [maintenance code map](../maintenance/menu-service.md) for paths and modification recipes. Keep JPA entities out of application results and HTTP responses.

## 7. Verification and acceptance evidence

Latest full verification on 2026-10-10: **101 reported passing cases**, comprising 75 Surefire and 26 Failsafe cases, with no failures/errors/skips. Four pre-existing methods are empty, and one audit-named unit test only checks saving. Counts are not equivalent to complete behavior coverage.

| Behavior | Current evidence |
|---|---|
| Domain validation and use-case orchestration | Unit tests |
| HTTP statuses, validation, request/result mapping | MockMvc tests |
| Name projection efficiency | One SQL statement, zero entity loads; empty input uses no SQL |
| Persistence and audit preservation | PostgreSQL integration tests |
| Commit/rollback after application transaction | Bulk integration tests outside a test transaction |
| Full create/read/update/delete flow | Real HTTP server and PostgreSQL, no mocked application beans |
| Real-startup audit registration | Real HTTP audit regression test |
| Simultaneous request races | Not verified |
| Load/latency, security, deployment | Not verified |

Run from `mealio-menu-service/` with Java 17 and Docker:

```bash
./mvnw -Dtest=CategoryControllerTest,CategoryMutationControllerTest test
./mvnw -Dtest=CategoryApiIT test
./mvnw clean verify
```

Reports: `target/surefire-reports/` and `target/failsafe-reports/`.

## 8. Pending features and completion criteria

| Priority | Work | Completion criteria |
|---|---|---|
| P1 | Schema and migration alignment | Identify current data/schema; explicit entity schema and named constraints; tested migration/upgrade; move toward validation |
| P1 | Concurrent conflict handling | Actual competing requests; recognized name constraint yields `409`; no partial writes; unrelated failures retain correct meaning |
| P2 | Pagination contract | Approved response shape, bounds, allowed sorting, stable order, meaningful tests |
| P2 | Error consistency | Stable codes and validation details; compatibility with the chosen API standard documented |
| P2 | Test cleanup | Replace/remove four empty tests; correct misleading audit test; improve domain boundary coverage |
| P2 | OpenAPI | Versioned schema matching observable behavior, including validation and errors |
| P2 | Administrative authorization | Approved permissions enforced and tested before external exposure |
| P3 | Activation/deactivation | Agreed lifecycle, use cases/endpoints, listing behavior, and tests |
| P3 | Category/menu-item relationship | Foreign-key and deletion rules; inactive-category policy; integration tests |
| P3 | Concurrent updates/deletes | Defined lost-update/race policy; locking/version contract if required; tests |
| Decision | Optional descriptions | Explicitly change domain, requests, mappers, schema, and tests together |
| Decision | Historical null audit values | Define a repair policy; do not invent historical actors/timestamps |

P1 is the next stabilization work; P2 completes contract and operational readiness; P3 supports broader menu features. These priorities are proposed planning guidance, not automatic implementation authorization.

## 9. Supporting records and future updates

- [Lookup TDD record](category-get-by-id.md)
- [Update/delete TDD record](category-update-delete.md)
- [Bulk validation record](category-bulk-validation.md)
- [Query optimization record](category-query-optimization.md)
- [Real HTTP verification and audit fix](category-api-verification.md)

For each modification: update the relevant contract section, add acceptance tests before implementation, record executed verification and limits, update the feature inventory/roadmap, and link the change record. Preserve dated historical test results rather than presenting them as current verification.
