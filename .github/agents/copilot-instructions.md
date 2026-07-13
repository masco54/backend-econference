# back-econference Development Guidelines

Auto-generated from all feature plans. Last updated: 2026-07-12

## Active Technologies
- Java 24 + Spring Boot 3.4.4, Spring Web, Spring Data JPA/Hibernate, Flyway, Jakarta Bean Validation, Lombok, Spring Security/JWT já existente em `mod-auth` (001-event-request-endpoints)
- H2 em memória para desenvolvimento/testes e PostgreSQL no profile persistente; banco físico único com migrações centralizadas em `application/src/main/resources/db/migration` e ownership funcional do dado em `mod-event` (001-event-request-endpoints)
- [ex.: Java 24 ou NEEDS CLARIFICATION] + [ex.: Spring Boot 3.4, Spring Security, Spring Data JPA ou NEEDS CLARIFICATION] (001-event-request-endpoints)
- [ex.: PostgreSQL + H2; banco físico único com schemas por módulo, ou NEEDS CLARIFICATION] (001-event-request-endpoints)
- Java 21 + Spring Boot 3, Spring Security (JWT via `mod-auth`), Spring Data JPA, Lombok (003-organizer-event-listing)
- H2 (dev/testes locais) / PostgreSQL (produção) — banco físico único, schema `events` de propriedade de `mod-event` (003-organizer-event-listing)

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
- 003-organizer-event-listing: Added Java 21 + Spring Boot 3, Spring Security (JWT via `mod-auth`), Spring Data JPA, Lombok
- 001-event-request-endpoints: Added [ex.: Java 24 ou NEEDS CLARIFICATION] + [ex.: Spring Boot 3.4, Spring Security, Spring Data JPA ou NEEDS CLARIFICATION]
- 001-event-request-endpoints: Added Java 24 + Spring Boot 3.4.4, Spring Web, Spring Data JPA/Hibernate, Flyway, Jakarta Bean Validation, Lombok, Spring Security/JWT já existente em `mod-auth`


<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
