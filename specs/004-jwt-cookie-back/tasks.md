# Tasks: JWT via Cookie HttpOnly — Backend Spring Boot

**Input**: Design documents from `/specs/004-jwt-cookie-back/`
**Prerequisites**: plan.md ✅, spec.md ✅, research.md ✅

## Phase 1: Setup

**Purpose**: Verificar que a implementação existente está correta antes de aplicar os gaps.

- [X] T001 Verificar que `mod-auth/src/main/java/com/econference/auth/controller/AuthController.java` emite cookie `auth_token` com `HttpOnly=true`, `Secure=true`, `SameSite=Lax` no login (leitura — sem alteração)
- [X] T002 Verificar que `mod-auth/src/main/java/com/econference/auth/security/JwtAuthenticationFilter.java` lê apenas do cookie `auth_token` e não do header `Authorization` (leitura — sem alteração)

---

## Phase 2: Foundational — Testes de base (US1 + US2)

**Purpose**: Criar a classe de teste de integração que cobre o fluxo principal de autenticação por cookie. Bloqueante para os testes de US3 e US4.

**Independent Test**: `./mvnw test -pl mod-auth -Dtest=AuthControllerIntegrationTest` deve passar após T003.

- [X] T003 [US1] [US2] Criar `application/src/test/java/com/econference/application/AuthControllerIT.java` com `@SpringBootTest` + `@AutoConfigureMockMvc` cobrindo:
  - `POST /api/v1/auth/login` com credenciais válidas → resposta contém `Set-Cookie` com `HttpOnly` e `auth_token`
  - `GET /api/v1/auth/me` com cookie válido → 200 com campos `id`, `email`, `roles`
  - `GET /api/v1/auth/me` sem cookie → 401

---

## Phase 3: User Story 3 — Logout remove o cookie corretamente (P1)

**Story goal**: `POST /auth/logout` emite `Set-Cookie: auth_token=; Max-Age=0; Secure; SameSite=Lax`.

**Independent Test**: `./mvnw test -pl mod-auth -Dtest=AuthControllerIntegrationTest#logout*` deve passar.

- [X] T004 [US3] Adicionar `.secure(true).sameSite("Lax")` ao `ResponseCookie` do método `logout()` em `mod-auth/src/main/java/com/econference/auth/controller/AuthController.java`
- [X] T005 [US3] Adicionar teste de integração em `AuthControllerIT.java`: `POST /auth/logout` com cookie válido retorna 204 e `Set-Cookie` com `Max-Age=0` e atributo `Secure`

---

## Phase 4: User Story 4 — CORS de produção configurado (P1)

**Story goal**: Frontend de produção consegue enviar requisições com `withCredentials: true`.

**Independent Test**: Verificar que `application-production.properties` contém a propriedade e que o teste de CORS passa.

- [X] T006 [P] [US4] Criar ou atualizar `application/src/main/resources/application-production.yml` adicionando `app.cors.allowed-origins: "${CORS_ALLOWED_ORIGINS:https://app.econference.com}"`
- [X] T007 [P] [US4] Adicionar teste em `AuthControllerIT.java`: CorsConfigurationSource contém `allowCredentials=true` e origens configuradas

---

## Phase 5: Polish & Validação

- [X] T008 Rodar `mvn test -pl application -Dtest=AuthControllerIT` e verificar que todos os testes passam (6/6)
- [ ] T009 [P] Rodar `mvn build -pl application` e verificar que build compila sem erro

---

## Dependencies

```
T001, T002           (setup — leitura, paralelos)
T003                 (foundational — bloqueante para T005 e T007)
T004 → T005          (US3 — fix antes do teste)
T006, T007           (US4 — paralelos, T007 depende de T003)
T008 → T009          (polish — após todos)
```

## Parallel Execution

- T001 e T002 podem ser lidos em paralelo
- T006 e T007 podem ser implementados em paralelo
- T008 e T009 podem rodar em paralelo

## Implementation Strategy

**MVP**: T001 → T002 → T003 (verificar que o que existe funciona com testes).
**Incremento 2**: T004 + T005 (US3 — 1 linha + teste).
**Incremento 3**: T006 + T007 (US4 — config de produção).
**Finalização**: T008 + T009.
