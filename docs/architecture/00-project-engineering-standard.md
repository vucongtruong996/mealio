# Mealio Project Engineering Standard

**Version:** 0.1
**Status:** Draft for review
**Date:** 2026-10-05
**Scope:** All Mealio services, frontend, integrations, infrastructure, and AI workflows

## 1. Purpose and how to use this document

This document explains the concepts and engineering practices used to build Mealio. Read the concept first, apply the rule to a small feature, and prove the behavior with tests. The immediate learning vehicle is `mealio-menu-service`.

“Must” identifies an essential correctness or boundary rule. “Should” identifies a default that can change with a documented reason. Proposed conventions require review before changing existing APIs or schemas. This draft does not automatically supersede approved documents.

Mealio starts with a coffee-shop menu and ordering workflow. Multi-tenancy, dynamic pricing, wearable integrations, advanced agents, and large-scale availability targets are future possibilities. Treat performance and availability figures in the overview as goals requiring measurement, not capabilities already demonstrated.

## 2. Document map and decision process

| Document | Purpose |
|---|---|
| [System overview](01-system-overview.md) | Business vision and service boundaries |
| [API standard](02-api-standard.md) | Public contracts and interface conventions |
| [Backend standard](03-backend-standard.md) | Java and backend practices |
| [AI architecture](04-ai-agent-architecture.md) | Agent responsibilities and controlled workflows |
| [Database design](05-database-design.md) | Data ownership and storage design |
| [gRPC contracts](06-grpc-contract.md) | Internal synchronous interfaces |
| [Event design](07-event-driven-design.md) | Asynchronous integration |
| [Deployment architecture](08-deployment-architecture.md) | Environments and deployment direction |

Source code and migrations show what exists today; an approved contract describes intended behavior. A mismatch is a tracked gap, not permission to silently change either side. Record decisions and reconcile affected documents before releasing a changed contract.

Use short architecture decision records under `docs/decisions/` when a choice has meaningful alternatives. Include context, decision, alternatives, consequences, and status. Example: `0001-category-name-uniqueness.md`.

## 3. Architecture and service ownership

**Concept:** A service boundary groups related business rules and data. Separate deployment alone does not create independence.

| Service | Owns |
|---|---|
| Menu | Categories, menu items, prices, dietary information, menu visibility |
| Inventory | Stock, adjustments, reservations, stock-derived availability |
| Order | Order lifecycle, purchased-item snapshots, checkout coordination |
| User | Profiles, preferences, nutrition goals |
| Gateway | External routing and selected cross-cutting concerns |
| AI | Recommendations, retrieval, orchestration of authorized tools |

Rules:

- Services must not read or write another service's tables.
- Local development may share a PostgreSQL instance using isolated service schemas. This provides logical ownership, not physical database isolation.
- Use service APIs or events for cross-service interaction.
- Shared contracts must not expose JPA entities or contain shared business logic.
- Implement complete menu-service features before adding distributed infrastructure without a concrete need.

**Mealio example:** An order stores a snapshot of the purchased item name and price so subsequent menu changes do not rewrite purchase history.

## 4. Clean Architecture inside a Java service

**Concept:** Keep business policy separate from HTTP transport and persistence technology.

| Layer | Responsibility | Example |
|---|---|---|
| Presentation | HTTP parsing, boundary validation, DTO mapping, status codes | `CategoryController` |
| Application | Coordinate a use case, repository calls, transaction boundary | `BulkCreateCategoryUseCaseImpl` |
| Domain | Business state, invariants, repository contracts | `Category`, `CategoryRepository` |
| Infrastructure | Implement persistence and external integrations | `CategoryPersistenceRepository` |

Runtime calls flow from controller to use case to repository adapter. Code dependencies should point toward business contracts: the adapter implements the domain repository interface.

- Controllers must not access JPA repositories directly.
- JPA entities must not become public API responses or application results.
- Domain behavior should avoid HTTP, JPA, and external-service dependencies.
- Use constructor injection and the established use-case interfaces.
- Separate `Request`, `Command`, domain model, persistence `Entity`, `Result`, and `Response` when their responsibilities differ.
- Current Spring `Page`/`Pageable` signatures are an accepted compromise; replacing them is not a prerequisite for completing Category.

**Example flow:** request → command → use case → domain model → repository adapter → database → result → response.

## 5. Technology baseline

| Technology | Role | Learning focus |
|---|---|---|
| Java 17 | Business implementation | Records, immutability, interfaces, exceptions |
| Spring Boot / Spring | Application setup and managed components | Beans, auto-configuration, dependency injection |
| Spring MVC | REST transport | Request mapping, conversion, validation |
| Spring Data JPA / Hibernate | Persistence | Entity state, persistence context, SQL timing |
| PostgreSQL | Durable storage | Constraints, transactions, concurrent access |
| Flyway | Schema evolution | Versioned, reproducible migrations |
| Maven wrapper | Build execution | Dependency and build lifecycle |
| JUnit 5, Mockito, AssertJ | Automated verification | Behavior tests and isolated collaborators |
| MockMvc / Testcontainers | HTTP and database verification | Slice tests and real PostgreSQL behavior |
| Docker Compose | Local infrastructure | Ports, volumes, reproducible startup |

Keep current dependency versions unless a change has a concrete compatibility or maintenance reason. React, Redis, gRPC, messaging, AWS, RAG, and MCP enter the implementation when their use cases require them. A planned technology is not an installed capability.

## 6. Spring Boot lifecycle

### 6.1 Startup and bean lifecycle

**Concept:** Spring creates and connects managed objects; Boot supplies conditional configuration based on dependencies and settings.

Conceptual startup sequence:

1. `main()` calls `SpringApplication.run(...)`.
2. Spring loads configuration and prepares the application context.
3. Component scanning and auto-configuration register bean definitions.
4. Spring creates beans, injects dependencies, and applies initialization and post-processing.
5. Datasource, migrations, JPA, and the embedded server initialize according to their dependencies.
6. Startup runners execute, if configured, and application readiness is signaled.

This is a learning outline; initialization is dependency-driven rather than one rigid ordering of every bean.

Controllers and services are generally singleton beans. They must not store mutable per-request state in instance fields. Keep that state in local variables or explicit request data. Bean destruction callbacks and context shutdown release resources.

### 6.2 Request lifecycle

```text
Client → Tomcat → Filters → DispatcherServlet
       → JSON conversion and configured validation
       → Controller → Use case → Persistence
       → Response conversion → Client
```

Exception handling converts known failures into the agreed HTTP error contract. Filters handle concerns such as authentication and correlation; domain rules remain outside filters and controllers.

### 6.3 Transaction and persistence lifecycle

Spring normally applies `@Transactional` through a proxy. Calling a transactional method through its Spring-managed proxy starts or joins a transaction. A call from another method on the same instance generally bypasses that interception.

```text
Begin transaction → Validate → Query → Change/save entities
                  → Flush SQL → Commit
Failure requiring rollback → Roll back transaction
```

- Application use cases should define local transaction boundaries.
- Default rollback behavior covers unchecked exceptions; checked exceptions need explicit consideration.
- `save()` does not guarantee immediate SQL execution. Constraint failures can occur at flush or commit.
- Hibernate dirty checking tracks changes to managed entities; detached domain objects are not automatically tracked.
- Lazy loading and entity serialization must not shape the public API accidentally.
- Avoid holding database transactions open during slow external calls.

**Verification:** Prove bulk rollback using the actual transactional bean and PostgreSQL, then inspect state outside the failed transaction.

## 7. Domain modeling and validation

**Concept:** An invariant is a rule that must remain true whenever a valid business object exists.

- Define required versus optional inputs explicitly.
- Validate request shape at the HTTP boundary and enforce essential invariants in the domain.
- Coordinate rules spanning records or repositories in application use cases.
- Define normalization before checking uniqueness. Decide whether name comparison is case-sensitive.
- Keep request, domain, and database length/nullability rules consistent.
- Use `BigDecimal` with an explicit currency and rounding policy for money.
- Keep unknown nutrition values distinct from zero.
- Define lifecycle transitions such as active, inactive, and unavailable before creating endpoints.

**Category example:** The request permits an absent description, but the current domain rejects it. Decide one rule, then update contracts and tests together.

## 8. Database migrations, auditing, and concurrency

**Concept:** Application checks produce useful feedback; database constraints protect persisted correctness.

- Flyway migrations must describe required tables, constraints, and indexes.
- Do not edit migrations already applied to shared environments; add a new migration.
- Use explicit names for important constraints so failures can be identified safely.
- Target Hibernate schema validation after migrations and entity mappings agree.
- Test initialization from an empty database and upgrades from the preceding schema.
- Verify the schema used by both Flyway and Hibernate; migration schema configuration alone does not prove entity queries use it.
- Store event timestamps consistently using `Instant` and PostgreSQL timestamp types with a documented UTC policy.
- Audit actors must come from trusted server-side identity or explicit job identity, not request-body fields.
- Keep audit fields outside current Category request/response models.

A read-before-write uniqueness check is vulnerable to concurrent requests. Preserve the database unique constraint and translate only recognized business constraint violations. Other integrity failures must retain their actual meaning.

Define bulk semantics explicitly: all-or-nothing, request ordering, empty input, duplicates, and maximum batch size. For Category, all-or-nothing without silently skipping records is the proposed policy.

Use optimistic locking when a demonstrated concurrent-update requirement needs protection; do not assume uniqueness constraints prevent lost updates.

## 9. API contracts and errors

**Concept:** An API is an agreement with callers, independent of internal class structure.

Before implementation, document:

- Method, path, authorization, and request schema.
- Required/optional fields and normalization rules.
- Success status and response schema.
- Error conditions, status codes, and machine-readable codes.
- Pagination, ordering, filtering, and size limits.
- Idempotency and concurrency semantics where relevant.

Use versioned resource paths such as `/api/v1/categories`. Pagination must expose metadata, and paginated queries should use deterministic ordering with a tie-breaker when needed.

Current Category conflicts use Spring `ProblemDetail`; the existing API standard specifies a different envelope. Select and document the project convention before changing responses. Also resolve the documented `422` validation status versus the existing expectation of `400` for request validation.

Known conflicts should return `409`, missing resources `404`, and malformed requests `400`. Unexpected errors must not expose SQL, stack traces, or credentials. Document actual behavior with version-controlled OpenAPI rather than claiming an unimplemented contract.

## 10. Testing standard

**Concept:** Test at the cheapest level that can prove the behavior; use integration tests for behavior that depends on infrastructure.

| Test level | Proves | Tools |
|---|---|---|
| Domain | Invariants and lifecycle transitions | JUnit, AssertJ |
| Use case | Orchestration and no writes after rejected input | Mockito, JUnit |
| HTTP slice | JSON, validation, statuses, response/error contracts | `@WebMvcTest`, MockMvc |
| Persistence | Mapping, constraints, SQL behavior | JPA tests, PostgreSQL Testcontainers |
| Application integration | Bean wiring, transactional rollback, concurrent outcomes | Spring context, PostgreSQL |

- Use `*Test` for unit/slice tests and `*IT` for new Failsafe integration tests.
- Prefer descriptive names and Given/When/Then structure.
- Ensure PostgreSQL tests disable embedded datasource replacement where necessary.
- H2 is useful for selected fast tests, but cannot establish PostgreSQL-specific correctness.
- Avoid tests that only repeat implementation details.
- No numeric coverage threshold currently exists; require meaningful success, failure, and boundary coverage.
- Report executed commands, failures, skipped tests, and environment limitations accurately.

From `mealio-menu-service/`, use `./mvnw test` for Surefire and `./mvnw clean verify` for the full build including Failsafe. Existing `*IntegrationTest` names run under Surefire; Docker is required by tests that actually use Testcontainers.

## 11. Security, configuration, and observability

**Concept:** A reliable service validates who may act, protects sensitive data, and provides evidence when something fails.

- Authenticate callers and authorize operations on the server before exposing administrative writes beyond local development.
- Keep environment secrets out of Git; committed local credentials are not deployment credentials.
- Use encrypted transport in deployed environments.
- Never log passwords, tokens, or sensitive user health data.
- Propagate correlation identifiers and record useful operation, status, and timing information.
- Define liveness and readiness separately as deployment needs mature.
- Establish measurable latency and reliability targets before optimizing or claiming compliance.
- Retry only transient failures within a bounded policy; mutating retries require idempotency protection.

## 12. Distributed and AI workflows

**Concept:** A local database transaction cannot atomically commit changes across independent services.

Use synchronous APIs when the caller needs an immediate answer. Use events for asynchronous reactions when eventual consistency is acceptable. Specify timeouts, duplicate delivery handling, ordering needs, and failure recovery.

When database changes and event publication must remain consistent, evaluate a transactional outbox. Introduce sagas or compensating actions only for an actual cross-service workflow.

AI must use validated service interfaces. LLM output is not authorization. Tool execution must enforce identity, permissions, input validation, resource limits, and relevant confirmation policies. Record data sources and distinguish generated estimates from verified facts. Core ordering and menu functionality should remain usable when AI is unavailable.

## 13. Feature preparation and delivery workflow

Before coding a feature, write a short design containing:

1. Business problem and scope.
2. Current behavior and desired behavior.
3. Acceptance examples and rejection cases.
4. Domain rules and lifecycle decisions.
5. API contract and compatibility impact.
6. Schema/migration changes.
7. Transaction, concurrency, and failure behavior.
8. Tests, observability, and rollout considerations.

Then work incrementally: inspect source → write a failing test → implement → verify → review. Use focused branches and `type(scope): description` commits. Do not commit or push until the user reviews and authorizes it.

### Definition of done

- Acceptance criteria implemented and relevant tests pass.
- No placeholder endpoints report false success.
- API documentation matches observable behavior.
- Database migrations and mappings agree.
- Authorization, validation, and error behavior are addressed for the scope.
- Configuration and local execution instructions are sufficient.
- Risks and intentionally deferred work are documented.
- Changes are reviewed before commit/push.

## 14. Menu-service learning and delivery sequence

| Step | Concept | Deliverable |
|---|---|---|
| 1 | Beans and HTTP lifecycle | Trace startup and one Category request |
| 2 | Reliable baseline | Existing tests run with correct configuration |
| 3 | Validation and normalization | Agreed description/name/batch rules |
| 4 | Transactions and uniqueness | Bulk conflicts and rollback verified |
| 5 | Schema ownership | Flyway constraints, audit/schema alignment |
| 6 | Complete resource lifecycle | Category detail, update, pagination, lifecycle APIs |
| 7 | Relationships and money | Menu-item model and CRUD use cases |
| 8 | Data quality | Ingredients, allergens, sourced nutrition |
| 9 | Service contracts and operation | Secure, observable integration-ready menu service |

### Decisions pending before Category stabilization

- Optional or required description?
- Case-sensitive or case-insensitive category names?
- Empty-batch policy and maximum batch size?
- `ProblemDetail` or the existing documented error envelope?
- Validation status convention (`400` versus `422`)?
- Current audit column names or a migration to `input_*`/`update_*`?
- Category deactivate, soft-delete, or physical-delete policy?

These decisions should be resolved in small feature designs. Approving this draft alone does not imply approval of every unresolved option.
