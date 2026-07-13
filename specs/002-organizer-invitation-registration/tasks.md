# Tasks: Cadastro de Organizador via Link de Ativação após Aprovação de Evento (Backend)

**Feature**: `002-organizer-activation-registration`  
**Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)  
**Generated**: 2026-07-11  
**Total tasks**: 27 | **Parallel opportunities**: 9

---

## Phase 1: Setup — Project Initialization

*Fundação de schema e contrato entre módulos. Deve ser completa antes de implementar qualquer serviço ou adapter.*

- [X] T001 Create migration `mod-auth/src/main/resources/db/migration/V{next}__create_organizer_activation_tokens.sql` with DDL from data-model.md: table `organizer_activation_tokens`, unique constraint on `token`, indexes on `email` and `event_request_id`
- [X] T002 Create `shared/src/main/java/com/econference/shared/contract/OrganizerActivationTokenPort.java` with interface, inner records `CreateOrganizerActivationTokenCommand` (eventRequestId, eventId, firstName, lastName, email, phone, eventName) and `OrganizerActivationTokenResult` (token, expiresAt)

---

## Phase 2: Foundational — mod-auth Domain and Persistence (Blocking)

*Entidade, port de repositório e adapter JPA devem existir antes de qualquer serviço de aplicação.*

- [X] T003 Create `mod-auth/src/main/java/com/econference/auth/domain/OrganizerActivationToken.java`: JPA entity extending `BaseEntity` with fields `token`, `firstName`, `lastName`, `email`, `phone`, `eventName`, `eventId`, `eventRequestId`, `expiresAt`, `usedAt` — `@Column(unique=true)` on `token`
- [X] T004 [P] Create `mod-auth/src/main/java/com/econference/auth/application/port/out/OrganizerActivationTokenRepositoryPort.java` with methods `save(OrganizerActivationToken)`, `findByToken(String)`, `existsByEmailAndUsedAtIsNull(String)`
- [X] T005 [P] Create `mod-auth/src/main/java/com/econference/auth/config/OrganizerActivationConfig.java` with `@ConfigurationProperties(prefix = "app.organizer-activation")` holding `int expiryDays` (default 7) and `String baseUrl`
- [X] T006 Create `mod-auth/src/main/java/com/econference/auth/adapter/out/persistence/OrganizerActivationTokenJpaRepository.java` Spring Data JPA repo with `findByToken(String)` and `existsByEmailAndUsedAtIsNull(String)`
- [X] T007 Create `mod-auth/src/main/java/com/econference/auth/adapter/out/persistence/OrganizerActivationTokenRepositoryAdapter.java` implementing `OrganizerActivationTokenRepositoryPort` delegating to `OrganizerActivationTokenJpaRepository`

---

## Phase 3: User Story 1 — Validate Activation Token (P1)

**Story goal**: `GET /api/v1/auth/organizer-activation/validate?token=` retorna dados pré-preenchidos para token válido ou razão de invalidade para token inválido/expirado/já utilizado.

**Independent test**: Exercitar `GET /api/v1/auth/organizer-activation/validate?token=<token>` com H2 em memória para cenários: válido, expirado, já utilizado, não encontrado — verificar respostas conforme spec.

- [X] T008 [US1] Create `mod-auth/src/main/java/com/econference/auth/application/port/in/ValidateOrganizerActivationTokenUseCase.java` with method signature returning a result record with `valid`, `reason`, `firstName`, `lastName`, `email`, `phone`, `eventName`
- [X] T009 [US1] Create `mod-auth/src/main/java/com/econference/auth/application/service/OrganizerActivationTokenService.java` implementing `ValidateOrganizerActivationTokenUseCase`: load token by value → check `usedAt` (already-used), check `expiresAt` (expired), return pre-filled data or reason enum
- [X] T010 [US1] Create `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/response/ValidateOrganizerActivationTokenResponse.java` record with fields: `valid`, `reason`, `firstName`, `lastName`, `email`, `phone`, `eventName`
- [X] T011 [US1] Create `mod-auth/src/main/java/com/econference/auth/adapter/in/web/OrganizerActivationController.java` with `@RestController`, `GET /api/v1/auth/organizer-activation/validate` endpoint accepting `@RequestParam String token`, delegating to `ValidateOrganizerActivationTokenUseCase`, returning `ResponseEntity<ValidateOrganizerActivationTokenResponse>` always `200`

---

## Phase 4: User Story 2 — Complete Organizer Registration (P1)

**Story goal**: `POST /api/v1/auth/organizer-activation/complete` cria conta com `emailVerified=true`, roles `[ORGANIZER, PARTICIPANT]`, consome o token e retorna `userId`, `roles`, `eventId`.

**Independent test**: Exercitar `POST /complete` com token válido → verificar usuário criado com roles corretos e token marcado como usado. Retestar com mesmo token → verificar 422. Testar com e-mail já existente → verificar 409.

- [X] T012 [US2] Create `mod-auth/src/main/java/com/econference/auth/application/port/in/CompleteOrganizerActivationUseCase.java` with method accepting `CompleteOrganizerActivationCommand` (token, password, phone) and returning `CompleteOrganizerActivationResult` (userId, roles, eventId)
- [X] T013 [US2] Create `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/request/CompleteOrganizerActivationRequest.java` record with `@NotBlank String token`, `@NotBlank @Size(min=8,max=100) String password`, `@Pattern(...) String phone`
- [X] T014 [US2] Create `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/response/CompleteOrganizerActivationResponse.java` record with `UUID userId`, `Set<String> roles`, `UUID eventId`
- [X] T015 [US2] Create `mod-auth/src/main/java/com/econference/auth/application/service/CompleteOrganizerActivationService.java` implementing `CompleteOrganizerActivationUseCase`: validate token (throw 422 if invalid/expired/used), check email uniqueness (throw `EmailAlreadyExistsException` → 409), create `User` with `emailVerified=true` and roles `{ORGANIZER, PARTICIPANT}`, mark token `usedAt=now()`, persist both in `@Transactional`
- [X] T016 [US2] Add `POST /api/v1/auth/organizer-activation/complete` endpoint to `OrganizerActivationController.java` accepting `@Valid @RequestBody CompleteOrganizerActivationRequest`, delegating to `CompleteOrganizerActivationUseCase`, returning `ResponseEntity<CompleteOrganizerActivationResponse>` with status `201`

---

## Phase 5: User Story 3 — Event Approval Generates Activation Token (P2)

**Story goal**: Quando o administrador aprova uma solicitação de evento em `mod-event`, o sistema gera automaticamente um `OrganizerActivationToken` em `mod-auth` e dispara o e-mail de ativação.

**Independent test**: Exercitar `POST /api/v1/events/requests/{id}/approve` como ADMIN e verificar que `organizer_activation_tokens` recebe uma nova linha com dados do solicitante, e que o e-mail de ativação é disparado (verificável via mock do `EmailSenderPort` ou MailHog local).

- [X] T017 [P] [US3] Create `mod-auth/src/main/java/com/econference/auth/application/port/in/CreateOrganizerActivationTokenUseCase.java` with method accepting the `CreateOrganizerActivationTokenCommand` from `OrganizerActivationTokenPort`
- [X] T018 [P] [US3] Add `sendOrganizerActivationEmail(...)` to `EmailSenderPort.java` and implement in `SpringMailEmailSenderAdapter.java`
- [X] T019 [US3] Create `mod-auth/src/main/resources/templates/email/organizer-activation-email.html` Thymeleaf template with: greeting using `firstName`, event name (`eventName`), activation link button (`activationUrl`), token expiry notice (7 days) — styled consistently with `verification-email.html`
- [X] T020 [US3] Add `createActivationToken()` behavior to `OrganizerActivationTokenService.java` (extends it to also implement `CreateOrganizerActivationTokenUseCase`): generate UUID token, persist `OrganizerActivationToken`, dispatch e-mail via `EmailSenderPort` using `@Async`
- [X] T021 [US3] Create `mod-auth/src/main/java/com/econference/auth/adapter/out/identity/OrganizerActivationTokenCreationAdapter.java` implementing `OrganizerActivationTokenPort` from `shared`, delegating to `CreateOrganizerActivationTokenUseCase`
- [X] T022 [US3] Create `mod-event/.../OrganizerActivationTokenEventAdapter.java` ... modify `EventRequestReviewService`
- [X] T023 [US3] Wire `OrganizerActivationTokenCreationAdapter` in `application/` composition root (auto-wired via `@Component` + `@SpringBootApplication` scan)

---

## Phase 6: Polish & Cross-Cutting Concerns

- [ ] T024 [P] Create `mod-auth/src/test/java/com/econference/auth/application/service/OrganizerActivationTokenServiceTest.java` with unit tests (Mockito): valid token returns data, expired token returns reason, used token returns reason, token not found returns not-found reason, token creation persists and returns result
- [ ] T025 [P] Create `mod-auth/src/test/java/com/econference/auth/application/service/CompleteOrganizerActivationServiceTest.java` with unit tests: success creates user with correct roles and emailVerified=true, duplicate email throws exception, invalid/used/expired token throws exception, transactional rollback on persistence failure
- [ ] T026 [P] Create `mod-auth/src/test/java/com/econference/auth/adapter/in/web/OrganizerActivationControllerTest.java` with MockMvc integration tests (H2): validate valid token returns 200 with data, validate expired returns 200 with reason, complete success returns 201, complete duplicate email returns 409, complete invalid token returns 422
- [ ] T027 Create `mod-event/src/test/java/com/econference/event/contract/OrganizerActivationTokenPortContractTest.java` verifying the contract between `mod-event` and `mod-auth` using `OrganizerActivationTokenCreationAdapter`, following pattern of `EventRequestAccountProvisioningPortContractTest.java`

---

## Dependencies

```
T001, T002 (parallelizable — independent foundation)
T002 → T003 → T004, T005 (parallelizable after T003)
T003, T006 → T007
T004, T007 → T008 → T009 → T010 → T011
T004, T007 → T012 → T013, T014 (parallelizable) → T015 → T016
T002 → T021 → T022 → T023
T009, T020 → T017, T018 (parallelizable) → T019 → T020
T016 → T024, T025, T026 (parallelizable after implementation)
T023 → T027
```

## Parallel Execution per Story

**Phase 1 (independent):** T001 and T002 in parallel

**Phase 2 (after T002):** T003 first; then T004, T005 in parallel; then T006, T007

**US1 (sequential in same controller/service):** T008 → T009 → T010 → T011

**US2 (mostly sequential):** T012 → T013, T014 in parallel → T015 → T016

**US3 (parallelizable pairs):** T017, T018 in parallel (both new methods) → T019 → T020 → T021 → T022 → T023

**Polish:** T024, T025, T026 in parallel after Phase 4; T027 after T023

## Implementation Strategy

**MVP (P1 only — US1 + US2):** Complete Phases 1–4 (T001–T016). Delivers working validation and registration completion endpoints. The token can be seeded manually in H2 console for testing.

**Full delivery:** Phases 1–6 (T001–T027). Includes event approval trigger, email sending, and full test coverage.

**Suggested order for single developer:** T001 → T002 → T003 → T004 → T005 → T006 → T007 → T008 → T009 → T010 → T011 → T012 → T013 → T014 → T015 → T016 → T017 → T018 → T019 → T020 → T021 → T022 → T023 → T024 → T025 → T026 → T027
