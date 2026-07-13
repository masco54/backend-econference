# Tasks: Solicitação de Eventos

**Input**: Documentos de design em `specs/001-event-request-endpoints/`
**Prerequisites**: `plan.md` e `spec.md` obrigatórios; `research.md`, `data-model.md`, `contracts/` e `quickstart.md` disponíveis

**Tests**: Testes automatizados são obrigatórios para esta funcionalidade porque a implementação altera contratos HTTP públicos e administrativos, persistência, segurança, regras de estado e fronteiras entre módulos.

**Organization**: As tarefas são agrupadas por user story para permitir implementação, teste e demonstração independentes de cada história.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Preparar dependências, propriedades e estrutura básica do módulo para a implementação do fluxo.

- [X] T001 Atualizar `mod-event/pom.xml` e `application/pom.xml` com as dependências de validation, mail e suporte de teste necessárias para os endpoints de solicitação de eventos
- [X] T002 [P] Criar os marcadores de pacote em `mod-event/src/main/java/com/econference/event/application/package-info.java`, `mod-event/src/main/java/com/econference/event/adapter/package-info.java`, `mod-event/src/main/java/com/econference/event/adapter/in/web/package-info.java` e `mod-event/src/main/java/com/econference/event/adapter/out/package-info.java`
- [X] T003 [P] Adicionar propriedades de tracking code, expiração de token e notificação da feature em `application/src/main/resources/application.yml`
- [X] T004 [P] Adicionar overrides de teste da feature em `application/src/test/resources/application-test.yml`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Entregar schema, modelo central, persistência e baseline de segurança dos quais todas as user stories dependem.

**⚠️ CRITICAL**: Nenhum trabalho de user story pode começar antes da conclusão desta fase.

- [X] T005 Criar a migração `application/src/main/resources/db/migration/V3__create_event_request_schema.sql` com tabelas `event_requests`, `event_request_status_history`, índices de rastreio e compatibilidade H2/PostgreSQL
- [X] T006 [P] Criar `mod-event/src/main/java/com/econference/event/domain/EventRequest.java`, `mod-event/src/main/java/com/econference/event/domain/EventRequestStatus.java` e `mod-event/src/main/java/com/econference/event/domain/EventRequestStatusHistory.java`
- [X] T007 [P] Criar `mod-event/src/main/java/com/econference/event/repository/EventRequestRepository.java` e `mod-event/src/main/java/com/econference/event/application/port/out/EventRequestRepositoryPort.java`
- [X] T008 [P] Implementar `mod-event/src/main/java/com/econference/event/adapter/out/persistence/EventRequestRepositoryAdapter.java` para encapsular consultas de duplicidade, tracking code, paginação e histórico
- [X] T009 [P] Criar `mod-event/src/main/java/com/econference/event/application/exception/DuplicateEventRequestException.java`, `mod-event/src/main/java/com/econference/event/application/exception/InvalidEventRequestStateException.java`, `mod-event/src/main/java/com/econference/event/application/exception/InvalidActivationTokenException.java` e ajustar `shared/src/main/java/com/econference/shared/exception/GlobalExceptionHandler.java` se necessário
- [X] T010 [P] Atualizar `mod-auth/src/main/java/com/econference/auth/config/SecurityConfig.java` para liberar `/public/event-requests/**` e proteger `/admin/event-requests/**` com autenticação válida
- [X] T011 [P] Criar o teste de fronteira arquitetural em `mod-event/src/test/java/com/econference/event/architecture/EventModuleArchitectureTest.java`
- [X] T012 Criar o teste de migração e ownership em `application/src/test/java/com/econference/application/EventRequestMigrationIT.java`

**Checkpoint**: Schema, agregado principal, baseline de persistência e baseline de segurança estão prontos para as histórias.

---

## Phase 3: User Story 1 - Enviar solicitação pública de evento (Priority: P1) 🎯 MVP

**Goal**: Permitir que um visitante envie uma nova solicitação pública de evento e receba um código de acompanhamento único.

**Independent Test**: Um visitante consegue chamar `POST /public/event-requests` com payload válido, receber `201 Created` com `trackingCode`, e tentativas duplicadas ou inválidas retornam os erros esperados sem afetar outras histórias.

### Tests for User Story 1 ⚠️

- [X] T013 [P] [US1] Adicionar teste MockMvc de contrato para `POST /public/event-requests` em `mod-event/src/test/java/com/econference/event/adapter/in/web/EventRequestPublicSubmissionControllerIT.java`
- [X] T014 [P] [US1] Adicionar testes de aplicação para validação de datas, duplicidade e geração de código em `mod-event/src/test/java/com/econference/event/application/service/SubmitEventRequestServiceTest.java`
- [X] T015 [P] [US1] Adicionar testes de persistência para unicidade de solicitações abertas e busca por tracking code em `mod-event/src/test/java/com/econference/event/adapter/out/persistence/EventRequestRepositoryAdapterIT.java`

### Implementation for User Story 1

- [X] T016 [P] [US1] Criar `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/request/SubmitEventRequestRequest.java` e `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/SubmitEventRequestResponse.java`
- [X] T017 [P] [US1] Criar `mod-event/src/main/java/com/econference/event/application/port/in/SubmitEventRequestUseCase.java` e `mod-event/src/main/java/com/econference/event/application/port/out/TrackingCodeGeneratorPort.java`
- [X] T018 [US1] Implementar `mod-event/src/main/java/com/econference/event/application/service/SubmitEventRequestService.java`
- [X] T019 [US1] Implementar `mod-event/src/main/java/com/econference/event/adapter/out/generator/TrackingCodeGeneratorAdapter.java` e concluir o wiring em `mod-event/src/main/java/com/econference/event/adapter/out/persistence/EventRequestRepositoryAdapter.java`
- [X] T020 [US1] Implementar `POST /public/event-requests` em `mod-event/src/main/java/com/econference/event/adapter/in/web/EventRequestPublicController.java`

**Checkpoint**: A User Story 1 está funcional e testável de forma independente como MVP da feature.

---

## Phase 4: User Story 2 - Acompanhar e ativar uma solicitação aprovada (Priority: P2)

**Goal**: Permitir consulta pública por tracking code/e-mail e ativação de uma solicitação já aprovada com token de uso único.

**Independent Test**: Com uma solicitação previamente aprovada em fixture, o solicitante consegue consultar `POST /public/event-requests/track`, validar `GET /public/event-requests/activate` e concluir `POST /public/event-requests/activate` sem depender da implementação da fila administrativa.

### Tests for User Story 2 ⚠️

- [X] T021 [P] [US2] Adicionar testes MockMvc de contrato para `POST /public/event-requests/track`, `GET /public/event-requests/activate` e `POST /public/event-requests/activate` em `mod-event/src/test/java/com/econference/event/adapter/in/web/EventRequestTrackingAndActivationControllerIT.java`
- [X] T022 [P] [US2] Adicionar testes de aplicação para acompanhamento, expiração de token, uso único e falhas de ativação em `mod-event/src/test/java/com/econference/event/application/service/EventRequestActivationServiceTest.java`
- [X] T023 [P] [US2] Adicionar testes de persistência para leitura de histórico e transições de token em `mod-event/src/test/java/com/econference/event/adapter/out/persistence/EventRequestActivationRepositoryIT.java`
- [X] T024 [P] [US2] Adicionar testes de contrato para o consumo do port de provisionamento em `mod-event/src/test/java/com/econference/event/contract/EventRequestAccountProvisioningPortContractTest.java`

### Implementation for User Story 2

- [X] T025 [P] [US2] Criar `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/request/TrackEventRequestRequest.java`, `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/request/ActivateEventRequestRequest.java`, `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/TrackEventRequestResponse.java`, `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/ValidateActivationTokenResponse.java` e `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/ActivateEventRequestResponse.java`
- [X] T026 [P] [US2] Publicar `shared/src/main/java/com/econference/shared/contract/EventRequestAccountProvisioningPort.java` e implementar `mod-auth/src/main/java/com/econference/auth/adapter/out/identity/AuthEventRequestAccountProvisioningAdapter.java`
- [X] T027 [P] [US2] Criar `mod-event/src/main/java/com/econference/event/application/port/in/TrackEventRequestUseCase.java`, `mod-event/src/main/java/com/econference/event/application/port/in/ValidateActivationTokenUseCase.java` e `mod-event/src/main/java/com/econference/event/application/port/in/ActivateEventRequestUseCase.java`
- [X] T028 [US2] Implementar `mod-event/src/main/java/com/econference/event/application/service/TrackEventRequestService.java` e `mod-event/src/main/java/com/econference/event/application/service/EventRequestActivationService.java`
- [X] T029 [US2] Atualizar `mod-event/src/main/java/com/econference/event/domain/Event.java`, `mod-event/src/main/java/com/econference/event/repository/EventRepository.java` e `mod-event/src/main/java/com/econference/event/service/EventService.java` para suportar vínculo do organizador e do `linkedUserId` na ativação
- [X] T030 [US2] Implementar `POST /public/event-requests/track`, `GET /public/event-requests/activate` e `POST /public/event-requests/activate` em `mod-event/src/main/java/com/econference/event/adapter/in/web/EventRequestPublicController.java`

**Checkpoint**: As jornadas de acompanhamento e ativação funcionam de forma independente sobre solicitações aprovadas em fixture.

---

## Phase 5: User Story 3 - Revisar solicitações como administrador (Priority: P3)

**Goal**: Permitir que administradores listem, detalhem, anotem, aprovem, rejeitem e reemitam token para solicitações de evento.

**Independent Test**: Um administrador autenticado consegue operar toda a superfície `/admin/event-requests/**` com paginação, filtros, decisões e reemissão de token usando fixtures do módulo, sem depender da jornada pública de submissão em runtime.

### Tests for User Story 3 ⚠️

- [X] T031 [P] [US3] Adicionar testes MockMvc para `GET /admin/event-requests`, `GET /admin/event-requests/{requestId}` e operações administrativas de decisão em `mod-event/src/test/java/com/econference/event/adapter/in/web/EventRequestAdminControllerIT.java`
- [X] T032 [P] [US3] Adicionar testes de aplicação para nota, aprovação, rejeição e reemissão de token em `mod-event/src/test/java/com/econference/event/application/service/EventRequestReviewServiceTest.java`
- [X] T033 [P] [US3] Adicionar testes de persistência para paginação, filtros, busca textual e carregamento de histórico em `mod-event/src/test/java/com/econference/event/adapter/out/persistence/EventRequestAdminQueryIT.java`
- [X] T034 [P] [US3] Adicionar regressões de autorização ADMIN para `/admin/event-requests/**` em `mod-event/src/test/java/com/econference/event/adapter/in/web/EventRequestAdminSecurityIT.java`

### Implementation for User Story 3

- [X] T035 [P] [US3] Criar `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/request/ApproveEventRequestRequest.java`, `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/request/RejectEventRequestRequest.java`, `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/request/AddEventRequestNoteRequest.java`, `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/AdminEventRequestListResponse.java` e `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventRequestResponse.java`
- [X] T036 [P] [US3] Criar `mod-event/src/main/java/com/econference/event/application/port/in/GetAdminEventRequestsUseCase.java`, `mod-event/src/main/java/com/econference/event/application/port/in/GetAdminEventRequestDetailUseCase.java`, `mod-event/src/main/java/com/econference/event/application/port/in/AddEventRequestNoteUseCase.java`, `mod-event/src/main/java/com/econference/event/application/port/in/ApproveEventRequestUseCase.java`, `mod-event/src/main/java/com/econference/event/application/port/in/RejectEventRequestUseCase.java`, `mod-event/src/main/java/com/econference/event/application/port/in/ReissueActivationTokenUseCase.java` e `mod-event/src/main/java/com/econference/event/application/port/out/EventRequestNotificationPort.java`
- [X] T037 [P] [US3] Implementar `mod-event/src/main/java/com/econference/event/adapter/out/notification/EventRequestMailNotificationAdapter.java` e `mod-event/src/main/java/com/econference/event/adapter/out/notification/EventRequestNotificationTemplates.java`
- [X] T038 [US3] Atualizar `mod-event/src/main/java/com/econference/event/domain/EventStatus.java`, `mod-event/src/main/java/com/econference/event/domain/Event.java` e `mod-event/src/main/java/com/econference/event/service/EventService.java` para criar o evento aprovado em estado de onboarding operacional e preencher `linkedEventId`
- [X] T039 [US3] Implementar `mod-event/src/main/java/com/econference/event/application/service/EventRequestReviewService.java`
- [X] T040 [US3] Implementar `GET /admin/event-requests`, `GET /admin/event-requests/{requestId}`, `POST /admin/event-requests/{requestId}/approve`, `POST /admin/event-requests/{requestId}/reject`, `POST /admin/event-requests/{requestId}/add-note` e `POST /admin/event-requests/{requestId}/reissue-token` em `mod-event/src/main/java/com/econference/event/adapter/in/web/EventRequestAdminController.java`

**Checkpoint**: A fila administrativa de solicitações está funcional e testável de forma independente.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Fechar documentação, compatibilidade de banco, performance e validação final da feature.

- [X] T041 [P] Atualizar `README.md`, `specs/001-event-request-endpoints/quickstart.md` e `event-requests.openapi.yaml` com exemplos finais e qualquer ajuste confirmado de comportamento
- [ ] T042 Validar rollback, compatibilidade H2/PostgreSQL e constraints de `application/src/main/resources/db/migration/V3__create_event_request_schema.sql` usando `application/src/test/resources/application-test.yml`
- [ ] T043 [P] Adicionar verificação de performance para submissão, tracking e listagem admin em `mod-event/src/test/java/com/econference/event/performance/EventRequestPerformanceIT.java`
- [X] T044 Executar a validação final da feature e registrar os resultados em `specs/001-event-request-endpoints/checklists/requirements.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1: Setup**: Sem dependências; pode começar imediatamente.
- **Phase 2: Foundational**: Depende da Phase 1 e bloqueia todas as user stories.
- **Phase 3: User Story 1**: Depende da conclusão da Phase 2.
- **Phase 4: User Story 2**: Depende da conclusão da Phase 2.
- **Phase 5: User Story 3**: Depende da conclusão da Phase 2.
- **Phase 6: Polish**: Depende da conclusão das user stories que farão parte da entrega.

### User Story Dependencies

- **User Story 1 (P1)**: Não depende das outras histórias após a base fundacional; é o MVP recomendado.
- **User Story 2 (P2)**: Não depende estritamente da US3; pode ser testada com solicitações aprovadas em fixture após a fase fundacional.
- **User Story 3 (P3)**: Não depende estritamente da US2; pode operar sobre fixtures do módulo e completar a fila administrativa de forma independente.

### Within Each User Story

- Testes devem ser escritos e falhar antes da implementação do comportamento novo.
- DTOs e ports vêm antes dos serviços que os consomem.
- Serviços vêm antes dos controllers/adapters HTTP.
- Persistência, segurança e validação devem ser verificadas antes de fechar a história.

### Parallel Opportunities

- `T002`, `T003` e `T004` podem rodar em paralelo no Setup.
- `T006`, `T007`, `T008`, `T009`, `T010` e `T011` podem rodar em paralelo na fase Foundational.
- Em cada user story, todas as tarefas de teste marcadas com `[P]` podem rodar em paralelo.
- Em cada user story, DTOs, ports e adapters marcados com `[P]` podem rodar em paralelo por tocarem arquivos diferentes.
- Após a conclusão da fase Foundational, as histórias US1, US2 e US3 podem ser desenvolvidas em paralelo por pessoas diferentes.

---

## Parallel Example: User Story 1

```bash
Task: "Adicionar teste MockMvc de contrato para POST /public/event-requests em mod-event/src/test/java/com/econference/event/adapter/in/web/EventRequestPublicSubmissionControllerIT.java"
Task: "Adicionar testes de aplicação para validação de datas, duplicidade e geração de código em mod-event/src/test/java/com/econference/event/application/service/SubmitEventRequestServiceTest.java"
Task: "Adicionar testes de persistência para unicidade de solicitações abertas e busca por tracking code em mod-event/src/test/java/com/econference/event/adapter/out/persistence/EventRequestRepositoryAdapterIT.java"

Task: "Criar SubmitEventRequestRequest.java e SubmitEventRequestResponse.java em mod-event/src/main/java/com/econference/event/adapter/in/web/dto/..."
Task: "Criar SubmitEventRequestUseCase.java e TrackingCodeGeneratorPort.java em mod-event/src/main/java/com/econference/event/application/port/..."
```

## Parallel Example: User Story 2

```bash
Task: "Adicionar testes MockMvc de contrato para track e activate em mod-event/src/test/java/com/econference/event/adapter/in/web/EventRequestTrackingAndActivationControllerIT.java"
Task: "Adicionar testes de contrato para o consumo do port de provisionamento em mod-event/src/test/java/com/econference/event/contract/EventRequestAccountProvisioningPortContractTest.java"
Task: "Criar TrackEventRequestRequest.java, ActivateEventRequestRequest.java e DTOs de resposta em mod-event/src/main/java/com/econference/event/adapter/in/web/dto/..."
Task: "Publicar EventRequestAccountProvisioningPort.java em shared/src/main/java/com/econference/shared/contract/ e implementar AuthEventRequestAccountProvisioningAdapter.java em mod-auth/src/main/java/com/econference/auth/adapter/out/identity/"
```

## Parallel Example: User Story 3

```bash
Task: "Adicionar testes MockMvc dos endpoints administrativos em mod-event/src/test/java/com/econference/event/adapter/in/web/EventRequestAdminControllerIT.java"
Task: "Adicionar testes de persistência para paginação, filtros e histórico em mod-event/src/test/java/com/econference/event/adapter/out/persistence/EventRequestAdminQueryIT.java"
Task: "Criar DTOs administrativos em mod-event/src/main/java/com/econference/event/adapter/in/web/dto/..."
Task: "Criar use cases administrativos e EventRequestNotificationPort.java em mod-event/src/main/java/com/econference/event/application/port/..."
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Concluir a Phase 1: Setup.
2. Concluir a Phase 2: Foundational.
3. Concluir a Phase 3: User Story 1.
4. Validar `POST /public/event-requests` de forma independente.
5. Demonstrar o MVP antes de seguir para ativação e revisão administrativa.

### Incremental Delivery

1. Setup + Foundational entregam a base persistente, de segurança e de arquitetura.
2. US1 entrega a entrada pública da funcionalidade e o código de acompanhamento.
3. US2 entrega a jornada do solicitante para acompanhamento e ativação.
4. US3 entrega a operação administrativa completa sobre a fila de solicitações.
5. Polish fecha documentação, performance e validação final.

### Parallel Team Strategy

1. O time conclui Setup + Foundational em conjunto.
2. Depois disso, uma pessoa pode seguir em US1, outra em US2 e outra em US3.
3. A integração final fica concentrada na fase de Polish, com validação cruzada e execução dos testes alvo.

---

## Notes

- Todos os itens seguem o formato obrigatório `- [ ] T### [P?] [US?] Descrição com caminho de arquivo`.
- Os testes foram incluídos porque a especificação e o plano exigem cobertura de contrato, integração, persistência, segurança e regressão.
- O escopo de MVP sugerido é a conclusão das Phases 1, 2 e 3.
- O contrato HTTP canônico continua sendo `event-requests.openapi.yaml`; mudanças de implementação não devem divergir dele sem atualização explícita dos artefatos.