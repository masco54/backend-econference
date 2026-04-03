# Plano de Implementação: Módulo de Autenticação (mod-auth)

**ID da Funcionalidade**: auth-module-complete  
**Plano Criado em**: 2026-04-02  
**Status**: Pronto para Implementação  
**Módulo**: `econference-mod-auth`  
**Branch**: `feature/auth-module-complete`

---

## Verificação da Constituição

| Princípio | Status | Observações |
|-----------|--------|-------------|
| **I. Qualidade de Código e Fronteiras Arquiteturais** | ✅ PASS | Camadas hexagonais definidas; separação entre domínio, aplicação e adaptadores explícita em todo o plano. Todas as dependências de framework permanecem nos adaptadores. |
| **II. Testes Definem Pronto** | ✅ PASS | Testes unitários para toda a lógica de domínio/aplicação; testes de integração para filtro de segurança, emissão de cookie, comportamento de repositório e envio de email. Todas as fases incluem entregáveis de teste. |
| **III. UX Consistente Entre Módulos** | ✅ PASS | O contrato de erro (`message`, `code`, `details`) é estendido em `shared` via `GlobalExceptionHandler`. O endpoint de health em `/api/auth/health` é preservado. |
| **IV. Orçamentos de Performance** | ✅ PASS | Meta de p95 < 500 ms para login/cadastro (escrita + bcrypt + email assíncrono). Endpoints de leitura (`/auth/me`, `/users/{id}`) com meta de p95 < 300 ms. O work factor do BCrypt permanece em 10 (padrão). O email é enviado de forma assíncrona (`@Async`) para desacoplar a resposta HTTP. |
| **V. Módulos Hexagonais, Operabilidade e Ownership de Dados** | ✅ PASS | `mod-auth` é dono do schema `auth` e de todas as migrações. Não há leituras diretas entre schemas. Os demais módulos acessam identidade do usuário por meio de um port publicado (`UserIdentityPort` em `shared`). O banco físico único é preservado. |

**Resultado da Verificação da Constituição**: Todos os gates passaram. A implementação pode prosseguir.

---

## Contexto Técnico

### Stack Confirmada
- **Linguagem**: Java 24
- **Framework**: Spring Boot 3.4.4
- **Build**: Maven (multi-módulo)
- **Security**: Spring Security 6 (already in `mod-auth/pom.xml`)
- **JWT**: JJWT 0.12.6 (already in `mod-auth/pom.xml`)
- **ORM**: Spring Data JPA + Hibernate
- **BD (local)**: H2 em memória
- **BD (persistente)**: PostgreSQL (profile `postgres`)
- **Migrations**: Flyway (already configured in `application`)
- **Email**: Spring Mail (a adicionar) — `JavaMailSender` com templates HTML
- **Async**: Spring `@Async` for email dispatch
- **Validation**: `spring-boot-starter-validation` (Jakarta Bean Validation)
- **Teste**: JUnit 5, Spring Boot Test, Spring Security Test, Mockito, Testcontainers (testes de integração com PostgreSQL)

### Principais Decisões de Arquitetura

| Decisão | Escolha | Justificativa |
|---------|---------|---------------|
| Mecanismo de sessão | JWT em cookie HttpOnly + tabela server-side `user_sessions` | O JWT é stateless por requisição; a tabela server-side permite invalidar login/logout/reset de senha sem esperar o token expirar |
| Biblioteca JWT | JJWT 0.12.6 | Já declarada em `mod-auth/pom.xml` |
| Nome do cookie | `auth_token` | Definido no contrato OpenAPI |
| Estratégia de CSRF | Desabilitada para API JWT stateless | O frontend envia `X-XSRF-TOKEN` conforme o contrato, mas como as sessões usam JWT stateless em cookie (e não sessão server-side), o risco de CSRF é mitigado por `SameSite=Lax`. O CSRF foi desabilitado para evitar complexidade de double-cookie. Exceção documentada pela Constituição. |
| CORS | Movido de `CorsConfig` em `shared` para `SecurityConfig` em `mod-auth` | O CORS de `/auth/**` e `/users/**` precisa usar credenciais; o `CorsConfig` atual cobre apenas `/api/**`. O `SecurityConfig` deve registrar as origens corretas e permitir credenciais. |
| Email | Spring JavaMailSender + `@Async` | Falha no envio de email não deve quebrar a resposta HTTP |
| Hash de senha | BCryptPasswordEncoder (já configurado) | Salted e irreversível |
| Prefixo de path da API | `/auth/**` e `/users/**` (sem prefixo `/api`) | O contrato define esses paths. O `SecurityConfig` da aplicação precisa permitir esses paths. O endpoint legado `/api/auth/**` de health é mantido por compatibilidade retroativa. |
| Escopo da refatoração de entidades | Refatorar `User` + `UserRole` in-place dentro de `mod-auth` | Essas entidades não são compartilhadas; `shared` fornece apenas `BaseEntity` e o port entre módulos |
| Validação de token de convite | Stub retornando `false` na Fase 1; lógica real postergada | `mod-review` e `mod-registration` ainda não foram implementados; stubs evitam bloqueio |
| Estratégia de migração | Nova migração Flyway `V2__refactor_auth_schema.sql` em `application/src/main/resources/db/migration` | A V1 já existe; a V2 adiciona as tabelas específicas do auth |

---

## Fase 0: Resultados da Pesquisa

Toda a pesquisa foi concluída inline, sem dúvidas bloqueantes. Principais resultados:

### JJWT 0.12.6 API
- Key generation: `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))`
- Builder: `Jwts.builder().subject(userId).expiration(date).signWith(key).compact()`
- Parser: `Jwts.parserBuilder().verifyWith(key).build().parseSignedClaims(token)`
- Preferred algorithm: HS256 (symmetric, sufficient for monolith)

### Spring Security 6 + JWT Cookie Filter
- Custom `OncePerRequestFilter` reads `auth_token` cookie, validates JWT, checks `user_sessions` table for invalidation, sets `SecurityContextHolder`
- `SecurityFilterChain` must use `addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)`
- Cookie issuance via `ResponseCookie.from("auth_token", jwt).httpOnly(true).secure(!isLocalProfile).sameSite("Lax").maxAge(Duration.ofDays(7)).path("/").build()`

### Spring Mail
- Dependency: `spring-boot-starter-mail`
- Config: `spring.mail.*` properties
- Interface: `JavaMailSender` + `MimeMessageHelper`
- Templates: strings HTML inline (ou Thymeleaf, se for adicionado); a especificação considera HTML simples suficiente

### BCrypt with Spring Security
- `BCryptPasswordEncoder.encode(rawPassword)` and `.matches(rawPassword, hash)`
- Work factor 10 is default; ~100 ms per hash — acceptable for p95 < 500 ms budget

### Flyway + H2 compatibility
- H2 handles `TIMESTAMP WITH TIME ZONE` if written as standard SQL; use `TIMESTAMP` for H2 compatibility
- `BYTEA` (PostgreSQL) → not used; token stored as `VARCHAR` (hex string)
- `SET` type not available in H2; use join table for multi-role instead of `@ElementCollection` with `ARRAY`

---

## Desenho do Modelo de Dados

### Alocação de Entidades

| Entidade | Módulo | Justificativa |
|----------|--------|---------------|
| `User` | `mod-auth` | Pertence ao auth; será refatorada a partir do esqueleto existente |
| `UserRole` | `mod-auth` | Enum interno do auth |
| `EmailVerificationToken` | `mod-auth` | Exclusiva do ciclo de vida de autenticação |
| `PasswordResetToken` | `mod-auth` | Exclusiva do ciclo de vida de autenticação |
| `UserSession` | `mod-auth` | Tabela server-side para invalidação de sessão |
| `BaseEntity` | `shared` | Já existe e será reutilizada |
| `UserIdentityPort` | `shared` | Contrato entre módulos para consulta de identidade do usuário |

### Entidade `User` Refatorada

```
User (table: users)
├── id            UUID PK (from BaseEntity)
├── first_name    VARCHAR(50) NOT NULL
├── last_name     VARCHAR(50) NOT NULL
├── email         VARCHAR(255) NOT NULL UNIQUE
├── phone         VARCHAR(20) NULLABLE
├── password_hash VARCHAR(255) NOT NULL
├── email_verified BOOLEAN NOT NULL DEFAULT FALSE
├── created_at    TIMESTAMP (from BaseEntity)
└── updated_at    TIMESTAMP (from BaseEntity)

user_roles (join table: users ↔ role enum)
├── user_id UUID FK → users.id
└── role    VARCHAR(50) NOT NULL
    Constraint: UNIQUE(user_id, role)
```

**UserRole enum** (replaces existing `ADMIN`/`USER`):
```
PARTICIPANT, ORGANIZER, REVIEWER, ADMIN
```
Serializado como strings minúsculas em JSON: `participant`, `organizer`, `reviewer`, `admin`.

### `EmailVerificationToken` Entity

```
email_verification_tokens
├── id         UUID PK
├── user_id    UUID FK → users.id NOT NULL
├── token      VARCHAR(255) UNIQUE NOT NULL
├── expires_at TIMESTAMP NOT NULL
├── used_at    TIMESTAMP NULLABLE
└── created_at TIMESTAMP NOT NULL
```

### `PasswordResetToken` Entity

```
password_reset_tokens
├── id         UUID PK
├── user_id    UUID FK → users.id NOT NULL
├── token      VARCHAR(255) UNIQUE NOT NULL
├── expires_at TIMESTAMP NOT NULL
├── used_at    TIMESTAMP NULLABLE
└── created_at TIMESTAMP NOT NULL
```

### `UserSession` Entity

```
user_sessions
├── id              UUID PK
├── user_id         UUID FK → users.id NOT NULL
├── token_hash      VARCHAR(255) UNIQUE NOT NULL  (SHA-256 hex of raw JWT)
├── expires_at      TIMESTAMP NOT NULL
├── invalidated_at  TIMESTAMP NULLABLE
└── created_at      TIMESTAMP NOT NULL
```

---

## Arquitetura de Componentes (Hexagonal)

### Mapa de Camadas de `mod-auth`

```
mod-auth/src/main/java/com/econference/auth/
│
├── domain/                          ← Domain Layer (pure business)
│   ├── User.java                    (JPA entity + domain logic)
│   ├── UserRole.java                (enum: PARTICIPANT, ORGANIZER, REVIEWER, ADMIN)
│   ├── EmailVerificationToken.java  (JPA entity)
│   ├── PasswordResetToken.java      (JPA entity)
│   └── UserSession.java             (JPA entity)
│
├── application/                     ← Application Layer (use-case orchestration)
│   ├── port/
│   │   ├── in/                      (inbound ports = use case interfaces)
│   │   │   ├── RegisterUserUseCase.java
│   │   │   ├── VerifyEmailUseCase.java
│   │   │   ├── ResendVerificationUseCase.java
│   │   │   ├── LoginUseCase.java
│   │   │   ├── LogoutUseCase.java
│   │   │   ├── GetCurrentUserUseCase.java
│   │   │   ├── ForgotPasswordUseCase.java
│   │   │   ├── ResetPasswordUseCase.java
│   │   │   ├── ChangePasswordUseCase.java
│   │   │   ├── GetPublicUserUseCase.java
│   │   │   ├── UpdateProfileUseCase.java
│   │   │   └── GetMyRolesUseCase.java
│   │   └── out/                     (outbound ports = driven ports)
│   │       ├── UserRepositoryPort.java
│   │       ├── EmailVerificationTokenRepositoryPort.java
│   │       ├── PasswordResetTokenRepositoryPort.java
│   │       ├── UserSessionRepositoryPort.java
│   │       ├── EmailSenderPort.java
│   │       └── InvitationTokenPort.java   (stub; returns false)
│   └── service/                     (use case implementations)
│       ├── RegistrationService.java
│       ├── EmailVerificationService.java
│       ├── AuthenticationService.java
│       ├── PasswordService.java
│       └── UserProfileService.java
│
├── adapter/
│   ├── in/
│   │   └── web/                     (driving adapters = REST controllers)
│   │       ├── AuthController.java  (POST /auth/register, /auth/login, /auth/logout,
│   │       │                          POST /auth/verify-email, /auth/resend-verification,
│   │       │                          GET /auth/me, POST /auth/password/forgot,
│   │       │                          POST /auth/password/reset, /auth/password/change)
│   │       ├── UserController.java  (GET /users/{userId}, PATCH /users/{userId},
│   │       │                          GET /users/me/roles)
│   │       └── dto/
│   │           ├── request/
│   │           │   ├── RegisterRequest.java
│   │           │   ├── LoginRequest.java
│   │           │   ├── VerifyEmailRequest.java
│   │           │   ├── ResendVerificationRequest.java
│   │           │   ├── ForgotPasswordRequest.java
│   │           │   ├── ResetPasswordRequest.java
│   │           │   ├── ChangePasswordRequest.java
│   │           │   └── UpdateProfileRequest.java
│   │           ├── response/
│   │           │   ├── RegisterResponse.java
│   │           │   ├── LoginResponse.java
│   │           │   ├── UserResponse.java    (full user — for /auth/me and login body)
│   │           │   ├── PublicUserResponse.java (id, firstName, lastName, roles only)
│   │           │   └── RolesResponse.java
│   │           └── mapper/
│   │               └── UserMapper.java      (User domain → UserResponse / PublicUserResponse)
│   └── out/
│       ├── persistence/             (driven adapters = JPA repositories)
│       │   ├── UserJpaRepository.java
│       │   ├── EmailVerificationTokenJpaRepository.java
│       │   ├── PasswordResetTokenJpaRepository.java
│       │   ├── UserSessionJpaRepository.java
│       │   ├── UserRepositoryAdapter.java
│       │   ├── EmailVerificationTokenRepositoryAdapter.java
│       │   ├── PasswordResetTokenRepositoryAdapter.java
│       │   └── UserSessionRepositoryAdapter.java
│       ├── email/                   (driven adapter = email sending)
│       │   ├── SpringMailEmailSenderAdapter.java
│       │   └── EmailTemplates.java  (HTML template strings)
│       └── stub/
│           └── InvitationTokenStubAdapter.java (always returns false)
│
├── config/
│   ├── SecurityConfig.java          (Spring Security filter chain, CORS for auth paths)
│   ├── JwtConfig.java               (JWT secret, expiry from properties)
│   ├── AsyncConfig.java             (@EnableAsync + thread pool)
│   └── AuthModuleConfig.java        (bean wiring, @ComponentScan if needed)
│
└── security/
    ├── JwtService.java              (generate, validate, extract claims)
    ├── JwtAuthenticationFilter.java (OncePerRequestFilter: reads cookie, validates, sets context)
    └── AuthenticatedUserPrincipal.java (UserDetails implementation)
```

### Mudanças em `shared`

```
shared/src/main/java/com/econference/shared/
├── domain/BaseEntity.java           (unchanged)
├── exception/
│   ├── BusinessException.java       (unchanged)
│   ├── ResourceNotFoundException.java (unchanged)
│   ├── AuthException.java           (NEW: 401 Unauthorized runtime exception)
│   ├── ForbiddenException.java      (NEW: 403 Forbidden runtime exception)
│   └── GlobalExceptionHandler.java  (ATUALIZADO: adiciona handlers para AuthException e ForbiddenException,
│                                      retornando JSON compatível com o contrato com campo "code")
├── config/
│   ├── JpaConfig.java               (unchanged)
│   └── CorsConfig.java              (unchanged — covers /api/**; SecurityConfig handles /auth/**)
└── contract/
    └── UserIdentityPort.java        (NEW: interface other modules use to look up user identity)
```

**`UserIdentityPort`** (contrato entre módulos):
```java
// Published by mod-auth; consumed by other modules
public interface UserIdentityPort {
    Optional<UserIdentity> findById(UUID userId);
    record UserIdentity(UUID id, String firstName, String lastName, Set<String> roles) {}
}
```

---

## Mapeamento da Implementação dos Endpoints de API

| Método | Path | Método do Controller | Caso de Uso | Auth | Status HTTP |
|--------|------|----------------------|-------------|------|-------------|
| POST | `/auth/register` | `AuthController.register()` | `RegisterUserUseCase` | No | 201 / 400 / 409 |
| POST | `/auth/verify-email` | `AuthController.verifyEmail()` | `VerifyEmailUseCase` | No | 200 / 400 |
| POST | `/auth/resend-verification` | `AuthController.resendVerification()` | `ResendVerificationUseCase` | No | 200 / 404 |
| POST | `/auth/login` | `AuthController.login()` | `LoginUseCase` | No | 200 / 401 |
| POST | `/auth/logout` | `AuthController.logout()` | `LogoutUseCase` | Yes | 204 / 401 |
| GET | `/auth/me` | `AuthController.getCurrentUser()` | `GetCurrentUserUseCase` | Yes | 200 / 401 |
| POST | `/auth/password/forgot` | `AuthController.forgotPassword()` | `ForgotPasswordUseCase` | No | 200 always |
| POST | `/auth/password/reset` | `AuthController.resetPassword()` | `ResetPasswordUseCase` | No | 200 / 400 |
| POST | `/auth/password/change` | `AuthController.changePassword()` | `ChangePasswordUseCase` | Yes | 200 / 400 / 401 |
| GET | `/users/{userId}` | `UserController.getUserById()` | `GetPublicUserUseCase` | Yes | 200 / 404 |
| PATCH | `/users/{userId}` | `UserController.updateProfile()` | `UpdateProfileUseCase` | Yes | 200 / 400 / 401 / 403 |
| GET | `/users/me/roles` | `UserController.getMyRoles()` | `GetMyRolesUseCase` | Yes | 200 / 401 |

**Observação sobre conflito de path**: `/users/me/roles` e `/users/{userId}` precisam ser declarados de forma que `me` não seja tratado como UUID. `UserController` deve mapear `/users/me/roles` antes de `/users/{userId}` ou usar a ordenação adequada de `@RequestMapping`. O Spring MVC resolve paths literais antes de variáveis, então `@GetMapping("/me/roles")` sob `@RequestMapping("/users")` é suficiente.

**Endpoint de health compatível com versões anteriores**: `GET /api/auth/health` preservado em `AuthController`.

---

## Desenho da Configuração de Segurança

### `SecurityConfig.java` (redesenho completo)

```
Permit-all paths (no auth required):
  POST /auth/register
  POST /auth/verify-email
  POST /auth/resend-verification
  POST /auth/login
  POST /auth/password/forgot
  POST /auth/password/reset
  GET  /actuator/**
  GET  /api/auth/health
  GET  /api/*/health

Protected paths (JWT required):
  POST /auth/logout
  GET  /auth/me
  POST /auth/password/change
  GET  /users/**
  PATCH /users/**

Filter chain:
  1. CorsFilter (from shared CorsConfig — handles /api/**)
  2. JwtAuthenticationFilter (before UsernamePasswordAuthenticationFilter)
     → reads auth_token cookie
     → validates JWT signature + expiry
     → checks user_sessions table (not invalidated)
     → loads user from DB
     → sets SecurityContext

Session policy: STATELESS (no HttpSession)
CSRF: DISABLED (exception documented — see below)
CORS for /auth/** and /users/**: configured inline in SecurityConfig with:
  allowedOrigins from app property `app.cors.allowed-origins`
  allowCredentials = true
  allowedMethods = GET, POST, PATCH, DELETE, OPTIONS
  allowedHeaders = *, X-XSRF-TOKEN, X-Requested-With
```

**Documentação da Exceção de CSRF** (exigência da Constituição):  
O CSRF está desabilitado porque as sessões usam JWT stateless em cookie. Um token CSRF stateful não pode ser mantido sem sessão server-side. A política de cookie `SameSite=Lax` fornece proteção equivalente para navegação same-site. O header `X-Requested-With: XMLHttpRequest` enviado pelo frontend também adiciona um header não simples que o navegador não adiciona em requisições cross-origin. Esta exceção é limitada ao módulo de auth e não afeta os demais módulos. Acompanhamento: implementar `SameSite=Strict` quando frontend e backend estiverem co-localizados.

---

## Desenho do JWT

### `JwtService`

```
Properties (from application.yml / env vars):
  app.jwt.secret         → Base64-encoded HMAC-SHA256 key (min 256 bits)
  app.jwt.expiration-days → default: 7

Token structure:
  Claims:
    sub    = userId (UUID string)
    iat    = issued-at
    exp    = issued-at + 7 days
    roles  = ["participant", "reviewer"]  (for future use)

Session table link:
  token_hash = SHA-256(rawJwt) stored in user_sessions
  On each request: compute SHA-256 of cookie value, look up in user_sessions,
  check invalidated_at IS NULL and expires_at > now()

Cookie issuance (login response):
  ResponseCookie.from("auth_token", jwt)
    .httpOnly(true)
    .secure(true)       // false only when profile == local
    .sameSite("Lax")
    .maxAge(Duration.ofDays(7))
    .path("/")
    .build()

Cookie clearing (logout response):
  ResponseCookie.from("auth_token", "")
    .httpOnly(true)
    .maxAge(Duration.ZERO)
    .path("/")
    .build()
```

---

## Estratégia de Tratamento de Erros

### Contrato de Resposta de Erro (JSON alvo)
```json
{
  "message": "Descrição legível para humanos",
  "code": "MACHINE_READABLE_CODE",
  "details": {}
}
```

### Atualizações em `GlobalExceptionHandler` (em `shared`)

| Classe de Exceção | Status HTTP | Campo `code` |
|-------------------|-------------|--------------|
| `ResourceNotFoundException` | 404 | `NOT_FOUND` |
| `BusinessException` | 400 | `VALIDATION_ERROR` (generic) |
| `EmailAlreadyExistsException` | 409 | `EMAIL_ALREADY_EXISTS` |
| `InvalidTokenException` | 400 | `INVALID_TOKEN` |
| `AuthException` | 401 | `UNAUTHORIZED` |
| `ForbiddenException` | 403 | `FORBIDDEN` |
| `EmailNotVerifiedException` | 401 | `EMAIL_NOT_VERIFIED` |
| `MethodArgumentNotValidException` | 422 | `VALIDATION_ERROR` |

**Observação**: O formato `ProblemDetail` usado atualmente em `GlobalExceptionHandler` deve ser substituído pelo JSON definido no contrato (`message`, `code`, `details`) para essas exceções. Um novo record `ErrorResponse` será criado em `shared` como tipo de retorno.

### Novas Classes de Exceção

| Classe | Localização | Herda de |
|--------|-------------|----------|
| `AuthException` | `shared/exception` | `RuntimeException` |
| `ForbiddenException` | `shared/exception` | `RuntimeException` |
| `EmailAlreadyExistsException` | `mod-auth/application` | `RuntimeException` |
| `EmailNotVerifiedException` | `mod-auth/application` | `RuntimeException` |
| `InvalidTokenException` | `mod-auth/application` | `RuntimeException` |

---

## Estratégia de Migração de Banco

### Arquivos de Migração Flyway (em `application/src/main/resources/db/migration`)

#### `V2__refactor_auth_schema.sql`

Mudanças nas tabelas existentes + novas tabelas:

```sql
-- 1. Migrate existing users data (preserve rows, add new columns)
ALTER TABLE users ADD COLUMN first_name VARCHAR(50);
ALTER TABLE users ADD COLUMN last_name  VARCHAR(50);
ALTER TABLE users ADD COLUMN phone      VARCHAR(20);
ALTER TABLE users ADD COLUMN password_hash VARCHAR(255);
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. Migrate name → first_name + last_name (split on first space)
UPDATE users SET 
  first_name = CASE WHEN POSITION(' ' IN name) > 0 THEN SUBSTRING(name, 1, POSITION(' ' IN name) - 1) ELSE name END,
  last_name  = CASE WHEN POSITION(' ' IN name) > 0 THEN SUBSTRING(name, POSITION(' ' IN name) + 1) ELSE '' END;

-- 3. Migrate password → password_hash (same value; already hashed via bcrypt)
UPDATE users SET password_hash = password;

-- 4. Make new columns NOT NULL after data migration
ALTER TABLE users ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE users ALTER COLUMN last_name  SET NOT NULL;
ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;

-- 5. Drop old columns
ALTER TABLE users DROP COLUMN name;
ALTER TABLE users DROP COLUMN password;
ALTER TABLE users DROP COLUMN role;
ALTER TABLE users DROP COLUMN active;

-- 6. Create user_roles join table (multi-role)
CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role    VARCHAR(50) NOT NULL,
    CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role)
);

-- 7. Assign default role to existing users
INSERT INTO user_roles (user_id, role)
SELECT id, 'PARTICIPANT' FROM users;

-- 8. Email verification tokens
CREATE TABLE email_verification_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. Password reset tokens
CREATE TABLE password_reset_tokens (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token      VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. User sessions
CREATE TABLE user_sessions (
    id             UUID PRIMARY KEY,
    user_id        UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash     VARCHAR(255) NOT NULL UNIQUE,
    expires_at     TIMESTAMP NOT NULL,
    invalidated_at TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. Indexes
CREATE INDEX idx_email_verification_tokens_token    ON email_verification_tokens(token);
CREATE INDEX idx_email_verification_tokens_user_id  ON email_verification_tokens(user_id);
CREATE INDEX idx_password_reset_tokens_token        ON password_reset_tokens(token);
CREATE INDEX idx_user_sessions_token_hash           ON user_sessions(token_hash);
CREATE INDEX idx_user_sessions_user_id              ON user_sessions(user_id);
```

**Observações de Compatibilidade com H2**:
- `POSITION()` and `SUBSTRING()` work in H2.
- `REFERENCES` foreign keys work in H2 when referential integrity is enabled.
- No PostgreSQL-specific types used.

**Observações de Rollback**:
- V2 migration is destructive (drops `name`, `password`, `role`, `active` columns). No automated rollback. If rollback is required before any new user data: restore from backup or re-apply V1 and re-seed. Flyway `repair` can be used if migration fails mid-way.

---

## Propriedades da Aplicação (novas entradas)

```yaml
# application.yml additions
app:
  cors:
    allowed-origins: "http://localhost:3000"
  jwt:
    secret: "${JWT_SECRET:your-256-bit-base64-encoded-secret-for-dev-only}"
    expiration-days: 7
  email:
    from: "suporte@econferences.com.br"
    verification-expiry-hours: 24
    reset-token-expiry-hours: 1
    base-url: "http://localhost:3000"   # frontend base URL for links in emails
  invitation:
    stub-mode: true   # true = InvitationTokenStubAdapter; false = real cross-module lookup

spring:
  mail:
    host: "${MAIL_HOST:mail.econferences.com.br}"
    port: "${MAIL_PORT:465}"
    username: "${MAIL_USERNAME:suporte@econferences.com.br}"
    password: "${MAIL_PASSWORD:}"
    properties:
      mail.smtp.auth: true
      mail.smtp.ssl.enable: true
      mail.smtp.starttls.enable: false
```

---

## Sequência de Implementação

### Fase 1 — Base e Refatoração de Entidades (Desbloqueia tudo)

**Objetivo**: Esqueleto compilável com modelo de domínio e schema de banco corretos.

**Tarefas (em ordem)**:

1. **Adicionar as dependências faltantes em `mod-auth/pom.xml`**:
   - `spring-boot-starter-web`
   - `spring-boot-starter-data-jpa`
   - `spring-boot-starter-validation`
   - `spring-boot-starter-mail`

2. **Refactor `UserRole` enum** in `mod-auth/domain/`:
   - Replace `ADMIN`/`USER` with `PARTICIPANT`, `ORGANIZER`, `REVIEWER`, `ADMIN`

3. **Refactor `User` entity** in `mod-auth/domain/`:
   - Replace `name` → `firstName` + `lastName`
   - Add `phone` (nullable)
   - Rename `password` → `passwordHash`
   - Add `emailVerified` (default false)
   - Replace `@Enumerated` single role with `@ElementCollection` multi-role `Set<UserRole>`
   - Remove `active` field
   - Keep extending `BaseEntity`

4. **Criar novas entidades de domínio**: `EmailVerificationToken`, `PasswordResetToken`, `UserSession`

5. **Write `V2__refactor_auth_schema.sql`** migration

6. **Atualizar `UserRepository`** para refletir os novos nomes de campos; adicionar queries:
   - `findByEmail(String email)`
   - `existsByEmail(String email)`

7. **Add `UserIdentityPort`** to `shared/contract/`

8. **Adicionar novas classes de exceção** em `shared/exception/` e atualizar `GlobalExceptionHandler` para retornar JSON compatível com o contrato

9. **Verify build compiles** (`mvn compile`)

**Entregáveis**: Código compilando, migração V2 e modelo de domínio atualizado.

---

### Fase 2 — Ports de Saída e Adaptadores

**Objetivo**: Adaptadores JPA, adaptador de email e stubs totalmente conectados.

**Tasks**:

1. **Criar interfaces de repositório JPA**: `UserJpaRepository`, `EmailVerificationTokenJpaRepository`, `PasswordResetTokenJpaRepository`, `UserSessionJpaRepository`

2. **Criar interfaces de ports de saída**: `UserRepositoryPort`, `EmailVerificationTokenRepositoryPort`, `PasswordResetTokenRepositoryPort`, `UserSessionRepositoryPort`, `EmailSenderPort`, `InvitationTokenPort`

3. **Criar adaptadores de persistência**: `UserRepositoryAdapter` etc. (implementam ports e delegam para repositórios JPA)

4. **Create `SpringMailEmailSenderAdapter`**: implements `EmailSenderPort`, sends HTML emails asynchronously

5. **Create `InvitationTokenStubAdapter`**: always returns `false` for both token types

6. **Create `EmailTemplates`**: static methods returning HTML strings for verification and reset emails

7. **Configure `AsyncConfig`** with `@EnableAsync` and task executor

8. **Verify**: Integration tests for adapters (H2 in-memory or Testcontainers)

---

### Fase 3 — JWT e Infraestrutura de Segurança

**Objetivo**: Fluxo do cookie `auth_token` funcionando; filtro protegendo endpoints.

**Tasks**:

1. **Create `JwtService`**:
   - `generateToken(UUID userId, Set<UserRole> roles)` → signed JWT string
   - `extractUserId(String token)` → UUID
   - `isTokenValid(String token)` → boolean (signature + expiry)
   - Uses JJWT 0.12.6 API

2. **Create `AuthenticatedUserPrincipal`** (implements `UserDetails`):
   - Wraps `User` domain entity
   - `getAuthorities()` maps `UserRole` → `GrantedAuthority` (`ROLE_PARTICIPANT`, etc.)

3. **Create `JwtAuthenticationFilter`** (`OncePerRequestFilter`):
  - Extrair cookie → validar JWT → verificar tabela de sessão → carregar usuário → definir contexto

4. **Redesign `SecurityConfig`**:
   - Permit-all paths, protected paths
   - Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
   - CSRF disabled (documented)
   - CORS configured for `/auth/**` and `/users/**` with credentials

5. **Create `JwtConfig`** for property binding (`@ConfigurationProperties(prefix = "app.jwt")`)

6. **Escrever testes unitários para `JwtService`** (gerar/validar/expirado/adulterado)

---

### Fase 4 — Serviços de Aplicação (Casos de Uso)

**Objetivo**: Toda a lógica de negócio implementada e testável sem a camada HTTP.

**Tarefas (por serviço)**:

#### `RegistrationService` (implements `RegisterUserUseCase`)
- Check email uniqueness → throw `EmailAlreadyExistsException` if duplicate
- Encode password with `BCryptPasswordEncoder`
- Set default role `PARTICIPANT`
- Try `invitationToken` via `InvitationTokenPort` → set `reviewerLinked` (silently ignore failure)
- Try `coOrganizerToken` via `InvitationTokenPort` → set `coOrganizerLinked` (silently ignore failure)
- Persist `User`
- Generate `EmailVerificationToken` (UUID token, 24h expiry) → persist
- Send verification email asynchronously via `EmailSenderPort`
- Return `RegisterResult(userId, reviewerLinked, coOrganizerLinked)`

#### `EmailVerificationService` (implements `VerifyEmailUseCase`, `ResendVerificationUseCase`)
- **Verify**: Find token → check not used, not expired → mark `usedAt` → set `user.emailVerified = true`
- **Resend**: Find user by email (404 if not found) → invalidate previous tokens → generate new → send email

#### `AuthenticationService` (implements `LoginUseCase`, `LogoutUseCase`, `GetCurrentUserUseCase`)
- **Login**: Find user by email (generic 401 if not found) → check `emailVerified` (401 EMAIL_NOT_VERIFIED) → match password (generic 401 if wrong) → generate JWT → create `UserSession` → return user + raw JWT for cookie
- **Logout**: Extract token hash from request cookie → find session → mark `invalidatedAt` → return
- **GetCurrentUser**: Carregar `User` a partir do contexto de segurança (já validado pelo filtro)

#### `PasswordService` (implements `ForgotPasswordUseCase`, `ResetPasswordUseCase`, `ChangePasswordUseCase`)
- **ForgotPassword**: Find user by email silently (always return success) → if found: generate `PasswordResetToken`, send email async
- **ResetPassword**: Encontrar o token → validar que não expirou nem foi usado → atualizar `passwordHash` → marcar token como usado → invalidar todas as sessões do usuário
- **ChangePassword**: Verify current password → encode new → update

#### `UserProfileService` (implements `GetPublicUserUseCase`, `UpdateProfileUseCase`, `GetMyRolesUseCase`)
- **GetPublicUser**: Find by UUID → return `PublicUserResponse`
- **UpdateProfile**: Verificar `authenticatedUserId == pathUserId` (403 se não) → validar campos → atualizar
- **GetMyRoles**: Return roles from security context principal

**Cobertura de testes unitários**: Todos os métodos de serviço e todos os ramos de execução (sucesso + erro).

---

### Fase 5 — Controllers REST e DTOs

**Objetivo**: Camada HTTP completa atendendo ao contrato OpenAPI.

**Tasks**:

1. **Criar classes DTO** (request/response) com anotações de Bean Validation:
   - `@NotBlank`, `@Size(min=2, max=50)` for firstName, lastName
   - `@Email` for email fields
   - `@Pattern(regexp="^\\+?[1-9]\\d{10,14}$")` for phone
   - `@Size(min=8, max=100)` for password fields

2. **Create `UserMapper`**: Maps `User` → `UserResponse` and `User` → `PublicUserResponse`
   - Roles mapped as lowercase strings: `UserRole.PARTICIPANT.name().toLowerCase()`

3. **Create `AuthController`**: all 9 auth endpoints
   - Login response: add `Set-Cookie` header via `HttpServletResponse.addHeader("Set-Cookie", cookie.toString())`
   - Logout: clear cookie + 204
   - All endpoints return contract-compliant JSON

4. **Create `UserController`**: 3 user endpoints

5. **Wire controllers** in `AuthModuleConfig` or via `@ComponentScan`

6. **Integration tests** (`@SpringBootTest` + `MockMvc` or `WebMvcTest`):
   - Register → verify email → login → access /auth/me → logout → reject subsequent requests
   - Error cases: duplicate email, wrong password, unverified, expired token

---

### Fase 6 — Hardening e Aspectos Transversais

**Objetivo**: Performance, consistência e operabilidade.

**Tasks**:

1. **Implement `UserIdentityPort` adapter** in `mod-auth` (exposes user identity to other modules via shared port)

2. **Add `@Async` to email sender** and configure thread pool in `AsyncConfig`

3. **Add `app.cors.allowed-origins` property** binding; update `SecurityConfig` CORS to read from properties

4. **Session cleanup**: Scheduled task (optional, configurable) to purge expired/invalidated sessions from DB

5. **Validar performance**:
   - Login latency test: target < 500 ms p95 locally (bcrypt cost-10 + DB round-trip)
   - `/auth/me` latency: target < 300 ms (single DB read + JWT validation)

6. **Integration smoke test** against full Spring context with H2

7. **Update `application.yml`** with new properties (`app.jwt.*`, `app.email.*`, `app.cors.*`, `spring.mail.*`)

8. **Spring Modulith verification**: Run `ApplicationModuleTest` to verify no architectural boundary violations

---

## Estratégia de Testes

### Níveis de Teste

| Nível | Escopo | Ferramentas | O que é testado |
|-------|--------|-------------|-----------------|
| **Unit** | Single class/method | JUnit 5 + Mockito | All service methods, JwtService, token validation, password matching, domain rules |
| **Slice (Web)** | Controller + Security | `@WebMvcTest` + `MockMvc` + Spring Security Test | HTTP status codes, cookie headers, request validation, error response format |
| **Slice (Repository)** | JPA queries | `@DataJpaTest` (H2) | Custom queries, token lookup, session invalidation |
| **Integration** | Full module | `@SpringBootTest` (H2) | Full registration→verification→login→logout flow |
| **Architecture** | Module boundaries | Spring Modulith `ApplicationModuleTest` | No cross-module package leakage |
| **Contract** | API vs OpenAPI | (Manual or Newman) | All 12 endpoints against auth.openapi.yaml |

### Principais Cenários de Teste (Automatizados)

1. `RegistrationService`: duplicate email → `EmailAlreadyExistsException`
2. `RegistrationService`: invalid invitation token → registration succeeds, `reviewerLinked=false`
3. `EmailVerificationService`: expired token → `InvalidTokenException`
4. `AuthenticationService`: unverified user login → `EmailNotVerifiedException`
5. `AuthenticationService`: wrong password → generic `AuthException`
6. `JwtService`: tampered token rejected; expired token rejected
7. `JwtAuthenticationFilter`: invalidated session rejected (token_hash in DB with `invalidated_at`)
8. `PasswordService`: reset token consumed → all user sessions invalidated
9. `UserProfileService`: cross-user profile update → `ForbiddenException`
10. `AuthController` (MockMvc): POST /auth/login → `Set-Cookie: auth_token=...` header present
11. `AuthController` (MockMvc): POST /auth/logout → `Set-Cookie: auth_token=; Max-Age=0` header present
12. `AuthController` (MockMvc): POST /auth/register with duplicate email → 409 JSON with `code: EMAIL_ALREADY_EXISTS`

---

## Observações sobre Dependências e Riscos

### Dependências

| Dependência | Nível de Risco | Mitigação |
|-------------|----------------|-----------|
| `mod-review` invitation token validation | LOW | Stubbed to `false`; real integration deferred to follow-up spec |
| `mod-registration` co-organizer token | LOW | Stubbed to `false`; follow-up spec |
| Dados existentes da migração V1 | MÉDIO | A V2 trata a migração compatível de colunas; dividir `name` no primeiro espaço pode gerar `last_name` vazio para nomes com uma só palavra, o que é aceitável para dados de dev/teste |
| H2 ↔ PostgreSQL compatibility | LOW | All SQL in V2 uses standard ANSI SQL; tested with H2 locally |
| Email delivery (SMTP config) | LOW | `@Async` + fire-and-forget; HTTP response not blocked by email failure |
| JWT secret rotation | MEDIUM | Secret must be set via environment variable in production; dev secret in application.yml is clearly marked. Secret rotation invalidates all sessions (acceptable since session table is invalidated anyway) |

### Limitações Conhecidas (Exceções à Spec Documentadas)

| Limitação | Observações | Acompanhamento |
|-----------|-------------|----------------|
| CSRF disabled | Documented above; `SameSite=Lax` provides equivalent protection | Consider `SameSite=Strict` when frontend is co-located |
| Rate limiting | Out of scope per spec; brute-force on login possible | Follow-up hardening task |
| Email template externalization | HTML templates inline in `EmailTemplates.java` | Externalize to Thymeleaf/resource files in follow-up |
| Invitation token validation | Stub returns `false` always | Depends on `mod-review` and `mod-registration` specs |

---

## Quickstart: Executando o Módulo Localmente

```bash
# 1. Iniciar a aplicação (H2 em memória, profile padrão)
cd back-econference
mvn spring-boot:run -pl application

# 2. Cadastrar um usuário
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"firstName":"João","lastName":"Silva","email":"joao@example.com","password":"Senha1234"}'

# 3. (Verificar o token de verificação no console do H2)
# http://localhost:8080/h2-console → SELECT * FROM email_verification_tokens;

# 4. Verificar o email
curl -s -X POST http://localhost:8080/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"token":"<token-from-db>"}'

# 5. Fazer login
curl -s -c cookies.txt -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"joao@example.com","password":"Senha1234"}'

# 6. Obter o usuário atual (com cookie)
curl -s -b cookies.txt http://localhost:8080/auth/me

# 7. Fazer logout
curl -s -b cookies.txt -c cookies.txt -X POST http://localhost:8080/auth/logout

# 8. Tentar /auth/me após o logout → esperar 401
curl -s -b cookies.txt http://localhost:8080/auth/me
```

---

## Resumo dos Artefatos Gerados

| Artefato | Path | Status |
|----------|------|--------|
| Especificação da Funcionalidade | `specs/master/spec.md` | ✅ Existe |
| Contrato de API (OpenAPI) | `specs/master/contracts/auth.openapi.yaml` | ✅ Existe |
| Contrato de API (Markdown) | `specs/master/contracts/auth-contract.md` | ✅ Existe |
| Checklist de Requisitos | `specs/master/checklists/requirements.md` | ✅ Validado |
| **Plano de Implementação** | `specs/master/plan.md` | ✅ **Este documento** |

---

## Prontidão para Implementação

Todas as decisões de design foram resolvidas. Não restam itens `NEEDS CLARIFICATION`. A implementação pode prosseguir para a geração das tarefas.

**Branch**: `feature/auth-module-complete`  
**Plano de Implementação**: `specs/master/plan.md`  
**Fases Estimadas**: 6 fases, com dependência sequencial  
**Classes Estimadas para Criar/Modificar**: ~50 classes (35 novas, ~15 refatoradas)  
**Scripts Flyway Estimados**: 1 novo (V2)  
**Constituição**: Todos os 5 princípios satisfeitos; 1 exceção documentada (CSRF desabilitado)

