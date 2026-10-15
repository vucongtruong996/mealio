# Repository Guidelines

## Project Structure & Module Organization

Mealio is a Java 17/Spring Boot microservices repository. `mealio-menu-service/` currently contains the implemented Maven application; other `mealio-*-service/` directories, `mealio-gateway/`, `mealio-contracts/`, and `lib/` are placeholders.

Menu code lives in `src/main/java/com/mealio/mealio_menu_service/`, organized into `domain`, `application`, `infrastructure`, and `presentation`. Tests mirror these packages under `src/test/java/`. Configuration and Flyway migrations belong in `src/main/resources/`; name migrations `V<number>__<description>.sql`. Infrastructure lives in `mealio-infra/`, and architecture guidance in `docs/architecture/`.

## Build, Test, and Development Commands

Use Java 17 and the committed Maven wrapper. From the repository root:

- `docker compose -f mealio-infra/docker/docker-compose.yml up -d`: start local PostgreSQL on port 5433.

Run these commands from `mealio-menu-service/`:

- `./mvnw clean verify`: compile, run tests and integration tests, and package the application.
- `./mvnw test`: run Surefire tests; existing `*IntegrationTest` classes also require Docker.
- `./mvnw -Dtest=CategoryTest test`: run a specific test class.
- `./mvnw spring-boot:run`: start the service on port 8080 after PostgreSQL is available.

## Coding Style & Naming Conventions

Match existing Java code: four-space indentation, PascalCase classes, camelCase methods and fields, and uppercase constants. Use descriptive suffixes such as `UseCase`, `UseCaseImpl`, `Request`, `Response`, `Entity`, and `Mapper`.

Use constructor injection. Keep business rules in the domain, orchestration and transactions in application use cases, persistence in infrastructure, and HTTP handling in presentation. Map between layers explicitly; never expose JPA entities through APIs. No formatter or lint plugin is currently configured. Consult `docs/architecture/03-backend-standard.md`, following the implemented package layout when its examples differ.

## Testing Guidelines

Use JUnit 5, Mockito, AssertJ, and Spring Boot test support. PostgreSQL integration tests use Testcontainers with PostgreSQL 17 and require Docker. Name unit tests `*Test` and new Failsafe integration tests `*IT`; use descriptive `should...` methods and Given/When/Then sections. Cover business rules, validation failures, mappings, and persistence behavior. No numeric coverage threshold is configured.

## Commit & Pull Request Guidelines

Follow the documented convention reflected in recent commits: `type(scope): description`, for example `feat(menu): add category persistence`. PRs should explain the behavior changed, link relevant issues, report test commands and results, and document API, migration, or configuration changes. Update affected documentation and obtain code review.

## Security & Configuration

Treat committed database credentials as local development defaults. Supply deployment secrets through environment configuration; never commit secrets or log tokens, passwords, or sensitive health data.

## AI Task Scope & Approval

Use the [harness skill](.agents/skills/harness/SKILL.md) for the development workflow,
review checkpoints, TDD, and documentation proportional to the task. Follow
[scope and approval rules](.ai/rules/scope.md): chat approval in review mode,
strict enforcement when selected, protection of existing edits, and explicit consequential
operation authorization. Keep process explanations out of routine review messages.
