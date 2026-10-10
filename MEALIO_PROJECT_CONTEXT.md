# Mealio — Project Context & Handover

**Purpose:** Transfer project history, architecture decisions, implementation status, and next steps into another ChatGPT/Codex account.

**Project owner:** Vu Cong Truong (Jack)
**Project:** Mealio — AI-Agentic Food Ordering & Nutrition Platform
**Last consolidated:** October 2026

---

## 1. Project Overview

Mealio is a food-ordering platform for a coffee shop that combines food and drink ordering with personalized nutrition recommendations and AI-agentic workflows.

**Important:** Mealio is an AI Agentic application, **not a chatbot**. The AI should perform useful tasks through controlled workflows and tools rather than simply answering conversationally.

### Goals
- Build a realistic, scalable, end-to-end application for Senior Software Engineer interview preparation.
- Demonstrate Java/Spring Boot microservices, Clean Architecture, domain-driven design, REST APIs, Docker, AWS, AI agents, LLMs, RAG, MCP, testing, and professional Git workflows.
- Provide a basic React frontend while keeping the main engineering focus on backend and AI architecture.

### Planned AI capabilities
- Recommend food and drinks based on party size, allergies, dietary restrictions, preferences, habits, and nutrition goals.
- Calculate or estimate nutritional information using appropriate data sources.
- Use order history and user preferences to improve future recommendations.
- Retrieve nutrition knowledge using RAG.
- Execute authorized application actions through MCP tools.
- Potentially assist with menu administration, such as proposing a menu item from a photo, with validation and authorization before saving.

Use multiple agents only when each has a clear responsibility and measurable benefit. Do not trust LLM output as authorization or as a substitute for backend validation.

---

## 2. Developer Profile and Interview Goals

Jack has:
- A Computer Science degree from Hanoi University of Industry.
- Around 7 years of software development experience, mainly with Java.
- Experience in system integration and enterprise systems.
- Experience in Android mobile development using Java and some React frontend experience.
- Professional experience operating, improving, and supporting Java integration services for HR systems, including batch synchronization, Workday/cloud integrations, databases, FTP/SFTP, production monitoring, root-cause analysis, and distributed-team collaboration.

Interview preparation targets Senior Java Engineer / Senior Software Engineer roles. Relevant topics include:
- Java, Spring Boot, microservices, REST, gRPC, AWS
- Distributed systems, scalability, reliability, system design
- Production troubleshooting, observability, technical decisions

Mealio should provide practical examples that Jack can explain naturally in interviews.

---

## 3. Working Preferences for the Assistant/Codex

### Coding
- Use Java and Spring Boot, Maven, Java records where suitable, and Lombok where it improves readability.
- Prefer constructor injection, clear naming, immutable data carriers, and maintainable code.
- Avoid unnecessary boilerplate, public setters, hard-coded business values, excessive abstractions, and overengineering.
- Follow the project's existing dependency versions and conventions unless a change is justified.

### Architecture
- Use a monorepo.
- Follow Clean Architecture principles and domain-driven design where practical.
- Separate domain, application, infrastructure, and presentation concerns.
- Keep business logic independent of persistence/framework details where possible.
- Do not leak JPA entities into domain/application APIs.
- Keep API request/response DTOs separate from domain models.
- Use application use-case interfaces and implementation classes, consistent with the existing package structure.

### TDD and collaboration
- Work incrementally, one meaningful step at a time.
- Write tests before or alongside implementation.
- Provide complete source code for every new or changed file, including package declarations and imports.
- Explain why the code is designed that way and what each test proves.
- Run relevant tests and resolve failures before proceeding.
- Inspect actual source code before assuming method signatures or constructors.
- Ask for missing code/logs when necessary rather than guessing.
- Do not commit or push until the user has reviewed the changes and tests pass.

### Git
- Use feature branches and focused commits.
- Verify `git status`, current branch, and recent commits before work.
- A branch named `menu-category` was previously mentioned, but current branch must be checked.

---

## 4. Technology and Monorepo

### Intended stack

| Area | Technology |
|---|---|
| Language | Java |
| Backend | Spring Boot |
| Build | Maven |
| Frontend | React |
| Database | PostgreSQL |
| Persistence | Spring Data JPA / Hibernate |
| Migrations | Flyway |
| Tests | JUnit 5, Mockito, Spring Boot Test |
| Simple test DB | H2, if appropriate |
| DB-specific integration tests | PostgreSQL / Testcontainers |
| Containers | Docker |
| Cloud | AWS |
| AI | LLM-based agents |
| Retrieval | RAG |
| Tool integration | MCP |
| Version control | Git |
| IDE | VS Code / Google Antigravity |

Use versions compatible with the current repository. Do not upgrade without a clear reason.

### Workspace structure

```text
mealio/
├── docs/
├── mealio-gateway/
├── mealio-menu-service/
├── mealio-order-service/
├── mealio-user-service/
├── mealio-inventory-service/
├── mealio-ai-service/
├── mealio-contracts/
└── mealio-infra/
```

### Service responsibilities
- **Gateway:** frontend/external entry point, routing, and appropriate cross-cutting concerns.
- **Menu service:** categories, menu items, food/drink information, availability, menu rules.
- **Order service:** order creation/lifecycle, order items, order history, integrations.
- **User service:** profiles, preferences, nutrition goals, user settings.
- **Inventory service:** stock quantities, availability, adjustments, inventory rules.
- **AI service:** agent orchestration, recommendations, nutrition assistance, RAG, MCP, LLM integration.
- **Contracts:** genuinely reusable integration contracts; avoid coupling services unnecessarily.
- **Infra:** Docker, local development infrastructure, deployment configuration, AWS infrastructure as the project matures.

---

## 5. Documentation

The `docs/` directory is intended to contain:
- System overview and architecture
- API standards
- Database design
- AI agent rules/skills
- Development conventions
- Deployment and operations guidance

Previously discussed overview document:
**“Mealio System Overview — AI-Agentic Food Ordering & Nutrition Platform”**
- Version: 1.0.0
- Status: Approved
- Includes LLMs, RAG over nutritional databases, and MCP servers.

A database-design Markdown document was previously created and pushed to Git. An API standards document named `02-api-standard.md` was discussed. Codex must inspect the actual files before making changes intended to comply with them; this handover does not reproduce their full contents.

---

## 6. Current Focus: `mealio-menu-service`

The active implementation focus has been the Category feature. Spring Boot was previously brought up successfully after resolving Maven and initial datasource configuration problems.

### Discussed package structure

```text
com.mealio.mealio_menu_service
├── domain/
│   ├── model/Category.java
│   ├── repository/CategoryRepository.java
│   └── exception/
├── application/
│   ├── mapper/CategoryApplicationMapper.java
│   ├── model/
│   │   ├── command/
│   │   └── result/
│   └── usecase/category/
│       ├── CreateCategoryUseCase.java
│       ├── ListCategoriesUseCase.java
│       ├── BulkCreateCategoryUseCase.java
│       └── impl/
├── infrastructure/
│   ├── persistence/
│   │   ├── entity/
│   │   │   ├── AuditableEntity.java
│   │   │   └── CategoryEntity.java
│   │   ├── mapper/CategoryPersistenceMapper.java
│   │   └── repository/
│   │       ├── JpaCategoryRepository.java
│   │       └── CategoryPersistenceRepository.java
│   └── audit/
└── presentation/
    ├── controller/CategoryController.java
    ├── mapper/CategoryPresentationMapper.java
    ├── request/
    └── response/
```

This is the previously discussed structure, not proof that every file currently exists. Inspect the repository before editing.

### Category domain
Business fields discussed:
- `id`
- `name`
- `description`
- `active`

Existing/requested exceptions:
- `InvalidCategoryException`
- `CategoryInactiveException`
- `CategoryNameCannotBeBlankException`

The domain was previously changed to use a `restore(...)` factory, with a simplified signature discussed as:

```java
Category.restore(id, name, description, active)
```

**Important:** Inspect the actual `Category.java` before writing code. Do not assume `Category.create(...)` exists; an earlier bulk example used it, but it may not match the current implementation.

---

## 7. Persistence and Auditing Decisions

### Desired audit columns
```text
input_user_id
input_dtm
update_user_id
update_dtm
```

- Actor ID is a `String`, representing either a frontend login user or batch job ID.
- Timestamps use `Instant` and should be stored consistently in UTC.
- Spring Data/JPA auditing should populate audit values before persistence.
- Do not accept audit fields from create/update request bodies.
- Do not expose audit fields in current Category API responses.
- An `AuditableEntity` base class was adopted/discussed.

Earlier entity examples used `created_by`, `created_at`, `updated_by`, and `updated_at`. The preferred naming later became `input_user_id`, `input_dtm`, `update_user_id`, and `update_dtm`. **Inspect current entities and Flyway migrations before changing names.** Do not rename DB columns without a migration plan.

### JPA auditing
`JpaAuditConfiguration` was discussed with:
- `@EnableJpaAuditing`
- `AuditorAware<String>`
- A current-actor provider

Inspect the current configuration/provider before changing it.

Valid UTC Hibernate configuration pattern:

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          time_zone: UTC
```

A prior YAML failure was caused by invalid indentation around `jdbc: time_zone: UTC`.

### Auditing tests
A `@DataJpaTest` previously failed because Spring tried to replace the configured datasource with an embedded DB, but none was available. Options:
- Add H2 with test scope for simple JPA tests.
- Use `@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)` with a configured test DB.
- Use PostgreSQL Testcontainers for PostgreSQL-specific behavior.

A previous test attempted `setDescription(...)` on `CategoryEntity`, but the entity intentionally had no setter. Do not add public setters just to satisfy a test. Test create auditing first, or implement intentional update behavior when the update use case exists.

---

## 8. Repository Layer Guidance

`CategoryPersistenceRepository` was discussed as an adapter between `CategoryRepository` and `JpaCategoryRepository`, using `CategoryPersistenceMapper`.

Architectural rule: the domain repository should return domain `Category`, not JPA `CategoryEntity`.

Preferred save behavior:

```java
@Override
public Category save(Category category) {
    CategoryEntity entity = categoryPersistenceMapper.toEntity(category);
    CategoryEntity saved = jpaCategoryRepository.save(entity);
    return categoryPersistenceMapper.toDomain(saved);
}
```

`deleteById(UUID id)` may be cleaner than converting a domain object back into an entity for deletion. If the current interface uses `delete(Category)`, inspect usage before changing it.

The project has used Spring Data `Page`/`Pageable` in repository/application signatures. This is acceptable for the current stage, though a custom pagination abstraction could reduce framework coupling later.

Bulk `saveAll(...)` was discussed with `Iterable<Category>` as a possible repository return type. A `List` is reasonable at the application boundary when ordered results matter. Keep interface and implementation signatures consistent.

---

## 9. Category API Contracts

### Single create
```http
POST /api/v1/categories
Content-Type: application/json
```

Example body:

```json
{
  "name": "Breakfast",
  "description": "Morning meals"
}
```

Previously discussed request record:

```java
public record CreateCategoryRequest(
    @NotBlank(message = "Category name must not be blank")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    String name,
    @Size(max = 500, message = "Category description must not exceed 500 characters")
    String description
) {}
```

Command:

```java
public record CreateCategoryCommand(
    String name,
    String description
) {}
```

Request/command models should contain business inputs only, not audit fields such as `inputUserId` or `updateUserId`.

### Bulk create
```http
POST /api/v1/categories/bulk
Content-Type: application/json
```

Example:

```json
[
  {"name": "Breakfast", "description": "Morning meals"},
  {"name": "Lunch", "description": "Afternoon meals"},
  {"name": "Dinner", "description": "Evening meals"}
]
```

A dedicated `/bulk` endpoint was preferred over allowing the normal endpoint to accept either an object or array.

Proposed use case interface:

```java
public interface BulkCreateCategoryUseCase {
    List<CategoryResult> execute(List<CreateCategoryCommand> commands);
}
```

Expected controller flow:
1. Validate request.
2. Map requests to commands.
3. Execute use case.
4. Map results to response DTOs.
5. Return `201 Created`.

Do not expose audit fields in the response.

### Bulk duplicate policy
Recommended policy: **all-or-nothing**.
- Duplicate name within request: reject.
- Name already in database: reject.
- Do not silently skip duplicate records.
- Keep operation transactional.
- Return `409 Conflict` for a category-name conflict.

Example error:

```json
{
  "status": 409,
  "error": "Conflict",
  "code": "CATEGORY_ALREADY_EXISTS",
  "message": "Category already exists: Breakfast"
}
```

This is a recommendation from the previous discussion. Verify whether it has been implemented and tested in the current source.

### List categories
Proposed endpoint:

```http
GET /api/v1/categories?page=0&size=20&sort=name,asc
```

Potential response:

```json
{
  "content": [
    {
      "id": "generated-uuid",
      "name": "Breakfast",
      "description": "Morning meals",
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1
}
```

A `ListCategoriesUseCase` returning `Page<CategoryResult>` was previously discussed. Inspect current code and API standards before finalizing the contract. `CreateCategoryRequest` does not need to change for listing.

---

## 10. Latest Error: Duplicate Category During Bulk Create

Observed PostgreSQL error:

```text
ERROR: duplicate key value violates unique constraint
Detail: Key (name)=(Breakfast) already exists.
```

The exception was:

```text
org.springframework.dao.DataIntegrityViolationException
```

The failing SQL attempted to insert into `categories`. The DB already had a row with name `Breakfast`, and the unique constraint correctly rejected the duplicate.

### Correct interpretation
- The request reached the application and persistence layer.
- PostgreSQL enforced the unique category-name constraint.
- This is a business conflict, not an HTTP parsing error.
- Do not remove the unique constraint.

### Recommended fix
1. Detect duplicate names within the incoming list before saving.
2. Check whether requested names already exist in the database.
3. Throw a specific exception such as `CategoryAlreadyExistsException`.
4. Map that exception to `409 Conflict` using the project's global exception handler.
5. Keep the DB unique constraint as the final safeguard for concurrent requests.
6. Map only the recognized category-name unique constraint violation to `409`; do not convert every `DataIntegrityViolationException` into a conflict.
7. Ensure transaction rollback prevents partial bulk insertion.

An `existsByName()` check followed by `save()` alone is not concurrency-safe. Two simultaneous requests may both pass the check, so the database constraint remains essential.

### Suggested exception

```java
public class CategoryAlreadyExistsException extends RuntimeException {
    public CategoryAlreadyExistsException(String name) {
        super("Category already exists: " + name);
    }
}
```

Use the actual project package and exception conventions.

---

## 11. Bulk API TDD Plan

Write tests first against the actual project types and signatures.

### Use case unit tests
- `shouldCreateMultipleCategories`
- `shouldRejectWhenCategoryAlreadyExists`
- `shouldRejectWhenRequestContainsDuplicateNames`
- `shouldNotSaveAnyCategoryWhenValidationFails`
- `shouldHandleEmptyRequestAccordingToContract`

### Controller tests
- Valid bulk request returns `201 Created`.
- Response contains all created categories.
- Blank names return `400 Bad Request`.
- Invalid JSON returns `400 Bad Request`.
- Empty array is rejected if contract requires at least one item.
- Duplicate-name exception returns `409 Conflict`.
- Use case is not called when request validation fails.

### Integration tests
- Persistence correctly saves a valid batch.
- Existing duplicate name triggers a conflict.
- Transaction rollback leaves no partial inserts.
- Concurrent uniqueness remains enforced by PostgreSQL.

Use `@WebMvcTest`/`MockMvc` for controller tests if consistent with the current test setup. Use H2 for simple JPA behavior or PostgreSQL Testcontainers for DB-specific constraints.

---

## 12. Other Known Issues

### Tomcat malformed HTTP request
Previously observed:

```text
Error parsing HTTP request header
Invalid character found in method name [{0x0a"event":"Book]
```

This means Tomcat received data that did not begin with a valid HTTP method. Possible causes:
- Raw JSON or non-HTTP data sent directly to the port.
- HTTPS sent to an HTTP port.
- Another service, proxy, health check, or client sending malformed traffic.

This is separate from the duplicate-name database error. Verify the client, protocol, and port if it happens again. It does not alone prove the controller is faulty.

### Lombok/JDT nullability warnings
Warnings included unchecked conversion to `@NonNull String` and `@NonNull UUID`. Inspect the exact annotation import and source before changing. Avoid adding nullability annotations everywhere simply to silence IDE warnings. Prefer API-boundary validation and domain invariants.

### Maven and startup
Maven was previously missing, then installed. The initial datasource configuration issue was also resolved and the menu service ran successfully. Verify current local setup instead of assuming it remains unchanged.

---

## 13. Development Roadmap

### Priority 1 — Stabilize Category bulk API
- Inspect current source, migrations, and tests.
- Confirm all-or-nothing duplicate behavior.
- Add unit tests.
- Implement request and database duplicate validation.
- Add specific conflict handling.
- Add controller tests.
- Add PostgreSQL integration tests.
- Run all menu-service tests.

### Priority 2 — Complete Category
- Verify single create.
- Verify bulk create.
- Verify paginated listing.
- Review validation and error response consistency.
- Review domain/persistence separation.
- Verify auditing and Flyway migrations.

### Priority 3 — Continue Menu Service
- Menu item domain and persistence.
- Menu item use cases and API.
- Category/menu-item relationship and business rules.
- Tests for all meaningful paths.

### Priority 4 — Build other services
- Inventory service
- User service
- Order service
- Gateway
- AI service
- Contracts and infrastructure

### Priority 5 — Platform and AI
- Docker workflows
- AWS deployment
- Observability and operational readiness
- Agent orchestration
- RAG and MCP integration
- Personalized nutrition and ordering workflows

---

## 14. Git Workflow

The project is a monorepo. The initial commit was previously pushed to `main`; `menu-category` was mentioned as a feature branch.

Current repository state is unknown and must be checked:
```bash
git status
git branch --show-current
git log -5 --oneline
```

Use focused feature branches and commits. Run tests before committing. Do not push until the user reviews and approves.

---

## 15. Instructions for Codex

You are helping Jack continue Mealio, a Senior Software Engineer interview-practice project.

1. Read this handover, then inspect the actual repository. The source code and migrations are the current source of truth.
2. First summarize the actual implementation and compare it with this handover. Identify completed, missing, and inconsistent parts.
3. Do not edit files until the user agrees on the proposed next step.
4. For the first task, focus on the Category bulk duplicate-name error.
5. Confirm actual class signatures, Category factory method, mapper methods, and repository return types before coding.
6. Use TDD and provide full source code for each changed/new file.
7. Preserve the database unique constraint and implement useful application-level conflict handling.
8. Do not silently skip duplicate items.
9. Keep audit fields out of API request/response models.
10. Follow the existing docs, API standards, and package conventions after inspecting them.
11. Explain the design trade-offs and relevant Senior Engineer considerations.
12. Run tests and report exact results.
13. Do not commit or push without user approval.

**First Codex prompt:**

> Read `MEALIO_PROJECT_CONTEXT.md` and inspect the current repository, especially `mealio-menu-service` and the Category bulk API. First summarize the actual implementation and compare it with the handover. Identify what is already implemented, what is missing, and any inconsistencies. Do not modify files yet. Then propose the next TDD step to fix the duplicate category name issue in the bulk API, following the architecture and coding preferences in the handover.
