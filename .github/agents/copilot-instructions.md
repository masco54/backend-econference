# back-econference Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-04-02

## Active Technologies

- Java 24 + Spring Boot 3.4, Spring Security 6, Spring Data JPA, Spring Mail, JJWT 0.12.6 (master / auth-module-complete)

## Project Structure

```text
back-econference/
├── shared/           # BaseEntity, shared exceptions, cross-module ports (UserIdentityPort)
├── mod-auth/         # Authentication module: Hexagonal (domain / application / adapter)
│   ├── domain/       # User, UserRole, EmailVerificationToken, PasswordResetToken, UserSession
│   ├── application/  # Use-case services, inbound + outbound port interfaces
│   └── adapter/      # REST controllers (AuthController, UserController), JPA adapters, email adapter
├── mod-event/
├── mod-committee/
├── mod-registration/
├── mod-review/
├── mod-submission/
└── application/      # Spring Boot entry point; Flyway migrations in src/main/resources/db/migration
```

## Architecture Principles

- Hexagonal (Ports & Adapters) per module; domain must not depend on Spring MVC / JPA directly
- Single physical database (PostgreSQL / H2 local); schema isolation per module
- Flyway for DB migrations (V1 exists; V2 adds auth tables)
- JWT in HttpOnly cookie (`auth_token`); server-side `user_sessions` table for invalidation
- `@Async` email dispatch (best-effort, non-blocking HTTP response)
- CSRF disabled (stateless JWT + SameSite=Lax); documented exception

## Commands

```bash
# Build all modules
mvn clean install -DskipTests

# Run application (H2 in-memory, default profile)
mvn spring-boot:run -pl application

# Run tests
mvn test

# Run with PostgreSQL profile
mvn spring-boot:run -pl application -Dspring-boot.run.profiles=postgres
```

## Code Style

Java 24: Follow standard Java conventions with Lombok (`@Getter`, `@Builder`, `@RequiredArgsConstructor`).
Use Bean Validation (`@NotBlank`, `@Size`, `@Email`, `@Pattern`) on all request DTOs.
Error responses must follow contract: `{ "message": "...", "code": "...", "details": {} }`.

## Recent Changes

- master (auth-module-complete): Added Java 24 + Spring Boot 3.4, Spring Security 6 (JWT cookie auth), JJWT 0.12.6, Spring Mail, multi-role User entity refactoring, Flyway V2 migration for auth schema

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
