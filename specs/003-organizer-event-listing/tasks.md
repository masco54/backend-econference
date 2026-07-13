# Tasks: Listagem de Eventos do Organizador (Back-end)

**Input**: Documentos de design em `/specs/003-organizer-event-listing/`
**Prerequisites**: [plan.md](./plan.md) · [spec.md](./spec.md) · [research.md](./research.md) · [data-model.md](./data-model.md) · [contracts/organizer-events.openapi.yaml](./contracts/organizer-events.openapi.yaml)

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Pode executar em paralelo (arquivos diferentes, sem dependências incompletas)
- **[US1]** / **[US2]**: User story da spec.md à qual a tarefa pertence

---

## Phase 1: Setup

**Purpose**: Verificar que a base está funcional antes de qualquer mudança

- [X] T001 Verificar que o build compila no branch `003-organizer-event-listing` executando `./mvnw clean compile -pl mod-event`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Contratos, DTOs e persistência que todas as user stories dependem

**⚠️ CRITICAL**: Nenhuma user story pode ser implementada antes desta fase estar concluída

- [X] T002 Mover `EventController` de `mod-event/src/main/java/com/econference/event/controller/EventController.java` para `mod-event/src/main/java/com/econference/event/adapter/in/web/EventController.java` alinhando ao padrão hexagonal do módulo (mesmo package dos demais controllers)
- [X] T003 [P] Criar `GetOrganizerEventsUseCase` com records `OrganizerListCommand(UUID organizerId, String status, int page, int limit)` e `OrganizerListResult(List<Event> data, Pagination pagination)` em `mod-event/src/main/java/com/econference/event/application/port/in/GetOrganizerEventsUseCase.java`
- [X] T004 [P] Criar `EventSummaryResponse` record com campos `id`, `name`, `status`, `startDate`, `endDate`, `location`, `createdAt` em `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventSummaryResponse.java`
- [X] T005 [P] Criar `OrganizerEventsResponse` record com `List<EventSummaryResponse> data` e `PaginationResponse pagination` (campos: `page`, `limit`, `total`, `totalPages`) em `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/OrganizerEventsResponse.java`
- [X] T006 Adicionar métodos `Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable)` e `Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatus status, Pageable pageable)` à interface `EventRepositoryPort` em `mod-event/src/main/java/com/econference/event/application/port/out/EventRepositoryPort.java`
- [X] T007 Adicionar métodos derivados `Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable)` e `Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatus status, Pageable pageable)` à interface `EventRepository` em `mod-event/src/main/java/com/econference/event/repository/EventRepository.java`
- [X] T008 Implementar os novos métodos de `EventRepositoryPort` em `EventRepositoryAdapter` delegando para `EventRepository` em `mod-event/src/main/java/com/econference/event/adapter/out/persistence/EventRepositoryAdapter.java`

**Checkpoint**: Contratos, DTOs e persistência prontos — implementação das user stories pode começar

---

## Phase 3: User Story 1 — Listar eventos do organizador autenticado (Priority: P1) 🎯 MVP

**Goal**: `GET /api/v1/events` retorna os eventos do organizador autenticado com paginação e filtro opcional por status

**Independent Test**: Chamar `GET /api/v1/events` com JWT de organizador com eventos cadastrados e verificar HTTP 200 com dados corretos e isolamento entre organizadores

### Tests for User Story 1

- [X] T009 [P] [US1] Escrever `OrganizerEventServiceTest` com Mockito cobrindo: lista com eventos, lista vazia, filtro por status, e que `organizerId` diferente não retorna eventos de outro organizador em `mod-event/src/test/java/com/econference/event/application/service/OrganizerEventServiceTest.java`
- [X] T010 [P] [US1] Escrever `EventControllerIT` MockMvc standalone cobrindo: HTTP 200 com eventos, HTTP 200 lista vazia, HTTP 400 com `status=INVALIDO`, verificação do shape de resposta `{ data, pagination }` em `mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerIT.java`

### Implementation for User Story 1

- [X] T011 [US1] Implementar `OrganizerEventService` implementando `GetOrganizerEventsUseCase`: extrair `EventStatus` do parâmetro string (lançar exceção para valor inválido), chamar repositório com `Pageable`, mapear `Page<Event>` para `OrganizerListResult` em `mod-event/src/main/java/com/econference/event/application/service/OrganizerEventService.java`
- [X] T012 [US1] Adicionar handler `GET /api/v1/events` ao `EventController` com `@PreAuthorize("hasRole('ORGANIZER')")`, `@AuthenticationPrincipal(expression = "userId") UUID organizerId`, `@RequestParam(required = false) String status`, `@RequestParam(defaultValue = "1") int page`, `@RequestParam(defaultValue = "20") int limit`; mapear resultado para `OrganizerEventsResponse` em `mod-event/src/main/java/com/econference/event/adapter/in/web/EventController.java`

**Checkpoint**: US1 funcional e testável de forma independente — `./mvnw test -pl mod-event -Dtest="OrganizerEventServiceTest,EventControllerIT"` deve passar

---

## Phase 4: User Story 2 — Acesso negado para usuário não autenticado (Priority: P2)

**Goal**: Requisições sem JWT válido recebem HTTP 401; requisições com JWT mas sem role `ORGANIZER` recebem HTTP 403

**Independent Test**: Chamar `GET /api/v1/events` sem `Authorization` header e verificar HTTP 401; chamar com token de participante e verificar HTTP 403

### Tests for User Story 2

- [X] T013 [US2] Escrever `EventControllerSecurityIT` como `@WebMvcTest` com `TestSecurityConfig` (padrão do módulo) cobrindo: 401 sem autenticação, 403 com role `PARTICIPANT`, 200 com role `ORGANIZER` em `mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerSecurityIT.java`

**Checkpoint**: US1 + US2 funcionais — `./mvnw test -pl mod-event -Dtest="OrganizerEventServiceTest,EventControllerIT,EventControllerSecurityIT"` deve passar

---

## Phase 5: Polish & Cross-Cutting Concerns

- [X] T014 [P] Executar suite completa do módulo `./mvnw test -pl mod-event` e corrigir qualquer falha de regressão introduzida pelo movimento de pacote do `EventController`
- [ ] T015 [P] Validar manualmente os comandos do `quickstart.md` com backend local rodando: login, `GET /api/v1/events` com token válido, `GET /api/v1/events` sem token

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: Sem dependências — começar aqui
- **Phase 2 (Foundational)**: Depende de Phase 1 — **bloqueia Phase 3 e 4**
- **Phase 3 (US1)**: Depende de Phase 2 — T009 e T010 [P] podem rodar em paralelo entre si; T011 depende de T009; T012 depende de T010 e T011
- **Phase 4 (US2)**: Depende de T012 (Phase 3 concluída) — T013 depende do controller com `@PreAuthorize` configurado
- **Phase 5 (Polish)**: Depende de Phase 4

### Parallel Execution (dentro de Phase 2)

T003, T004, T005 podem rodar em paralelo (arquivos distintos, sem dependências entre si).  
T006 depende de T003 (precisa do tipo `Event`). T007 depende de T006. T008 depende de T007.

### Parallel Execution (dentro de Phase 3)

T009 e T010 podem rodar em paralelo (arquivos de teste distintos).  
T011 depende de T009 (testes guiam a implementação).  
T012 depende de T010 e T011.

### Suggested MVP Scope

Implementar apenas **Phase 1 + Phase 2 + Phase 3** entrega o MVP completo: endpoint funcional com dados reais, paginação, filtro por status e testes unitários/integração.  
**Phase 4** adiciona cobertura de segurança explícita (testes de 401/403). Recomendado antes de merge.

---

## Task Summary

| Phase | Tasks | Parallelizable |
|-------|-------|---------------|
| 1 Setup | T001 | — |
| 2 Foundational | T002–T008 (7) | T003, T004, T005 em paralelo |
| 3 US1 (P1) | T009–T012 (4) | T009, T010 em paralelo |
| 4 US2 (P2) | T013 (1) | — |
| 5 Polish | T014–T015 (2) | T014, T015 em paralelo |
| **Total** | **15 tarefas** | |
