# Especificação de Funcionalidade: API de Gerenciamento de Evento

**Feature Branch**: `005-event-manage-api`
**Created**: 2026-07-13
**Status**: Draft
**Input**: Implementar endpoints de gerenciamento de evento — GET evento individual, categorias, eixos temáticos, modalidades e configurações necessárias para o EventAreaLayoutComponent funcionar

## User Scenarios & Testing *(obrigatório)*

### User Story 1 - Organizador obtém os detalhes completos de um evento (Priority: P1)

O sistema retorna todos os dados de um evento individual quando o organizador acessa sua área de gerenciamento, incluindo configurações de inscrição, submissão e avaliação.

**Why this priority**: É o bloqueador crítico. Sem `GET /api/v1/events/{id}`, o `EventAreaLayoutComponent` falha com 404 e redireciona de volta para a lista — tornando toda a área de gerenciamento inacessível.

**Independent Test**: Chamar `GET /api/v1/events/{id}` autenticado como organizador do evento → verificar resposta 200 com estrutura `{ data: EventDetailResponse }` contendo nome, status, datas, tipo, tema e campos de configuração.

**Acceptance Scenarios**:

1. **Given** um organizador autenticado, **When** `GET /api/v1/events/{id}` com ID de evento próprio, **Then** retorna 200 com `{ data: { id, name, status, type, theme, startDate, endDate, location, configurationCompleted, hasRegistration, hasSubmission, hasSite, ... } }`.
2. **Given** um organizador autenticado, **When** `GET /api/v1/events/{id}` com ID de evento de outro organizador, **Then** retorna 403 ou 404.
3. **Given** um ID de evento inexistente, **When** `GET /api/v1/events/{id}`, **Then** retorna 404 com mensagem "Event not found".
4. **Given** requisição sem autenticação (sem cookie), **When** `GET /api/v1/events/{id}`, **Then** retorna 401.

---

### User Story 2 - Listagem de eventos retorna dados completos para o frontend (Priority: P1)

A resposta de `GET /api/v1/events` inclui `type`, `theme`, `logoUrl` e `configurationCompleted` para que o frontend exiba corretamente o tipo do evento e controle o botão "Publicar".

**Why this priority**: Sem esses campos, a lista exibe colunas vazias e o botão "Publicar" fica habilitado indevidamente para todos os eventos aprovados (porque `undefined !== false`).

**Independent Test**: Chamar `GET /api/v1/events` como organizador → verificar que cada item na lista contém `type`, `theme`, `logoUrl` e `configurationCompleted`.

**Acceptance Scenarios**:

1. **Given** um organizador com eventos, **When** `GET /api/v1/events`, **Then** cada item retorna `type`, `theme`, `logoUrl` (nullable) e `configurationCompleted` (boolean).
2. **Given** evento com `configurationCompleted = false`, **When** a listagem retorna esse evento, **Then** o frontend desabilita o botão "Publicar" para ele.

---

### User Story 3 - Endpoints de configuração do evento (categorias, eixos temáticos, modalidades) (Priority: P2)

O sistema fornece endpoints para listar e gerenciar categorias de inscrição, eixos temáticos e modalidades de submissão de um evento, permitindo que o organizador configure o evento na área de gerenciamento.

**Why this priority**: O `EventAreaLayoutComponent` usa `forkJoin` para calcular o checklist de configuração. Se esses endpoints não existirem, o checklist sempre mostrará estado incompleto, mesmo que o organizador configure tudo.

**Independent Test**: Chamar `GET /api/v1/events/{id}/registration/categories` como organizador → verificar lista (possivelmente vazia) retornada com 200.

**Acceptance Scenarios**:

1. **Given** organizador autenticado com evento próprio, **When** `GET /api/v1/events/{id}/registration/categories`, **Then** retorna 200 com lista de categorias (vazia ou preenchida).
2. **Given** organizador autenticado com evento próprio, **When** `GET /api/v1/events/{id}/theme-axes`, **Then** retorna 200 com lista de eixos temáticos.
3. **Given** organizador autenticado com evento próprio, **When** `GET /api/v1/events/{id}/submission/modalities`, **Then** retorna 200 com lista de modalidades.
4. **Given** organizador sem acesso ao evento, **When** qualquer endpoint de configuração do evento, **Then** retorna 403 ou 404.

---

### User Story 4 - Endpoint de estatísticas do dashboard do evento (Priority: P2)

O dashboard do evento exibe contagens de inscritos e submetidos. O backend fornece um endpoint agregado com essas estatísticas.

**Why this priority**: Sem esse endpoint, o dashboard mostra zeros/erros em todos os cards de estatísticas.

**Independent Test**: Chamar `GET /api/v1/events/{id}/statistics` como organizador → verificar retorno com campos `registrationsCount`, `submissionsCount`, `reviewersCount`.

**Acceptance Scenarios**:

1. **Given** evento com inscrições e submissões, **When** `GET /api/v1/events/{id}/statistics`, **Then** retorna contagens corretas para cada categoria.
2. **Given** evento sem nenhuma inscrição, **When** `GET /api/v1/events/{id}/statistics`, **Then** retorna zeros (não erro).

---

### Edge Cases

- O que acontece quando um co-organizador (não principal) acessa `GET /api/v1/events/{id}`? → Deve retornar 200 com os mesmos dados (co-organizadores têm acesso de leitura).
- O que acontece quando `configurationCompleted` ainda não tem valor definido (evento recém-criado)? → Deve retornar `false` por padrão.
- O que acontece quando o banco de dados contém eventos sem `type` ou `theme` (legados)? → Deve retornar `null` nesses campos, sem erro.
- O que acontece quando carga de listagem retorna muitos eventos? → `GET /api/v1/events` deve suportar paginação (page, limit) já implementada.
- O que acontece quando o módulo de inscrições não está configurado (`hasRegistration = false`)? → `GET /api/v1/events/{id}/registration/categories` pode retornar lista vazia ou indicar que inscrições não estão habilitadas.

## Requirements *(obrigatório)*

### Functional Requirements

- **FR-001**: O sistema MUST implementar `GET /api/v1/events/{id}` que retorna o evento completo no formato `{ data: EventDetailResponse }`, acessível apenas pelo organizador principal, co-organizadores do evento ou admin.
- **FR-002**: `EventDetailResponse` MUST conter: `id`, `name`, `acronym`, `description`, `status`, `type`, `theme`, `startDate`, `endDate`, `location`, `organizerId`, `configurationCompleted`, `hasRegistration`, `hasSubmission`, `hasSite`, `logoUrl`, `registrationStartDate`, `registrationEndDate`, `submissionStartDate`, `submissionEndDate`.
- **FR-003**: `EventSummaryResponse` (retornado em `GET /api/v1/events`) MUST ser expandido para incluir `type`, `theme`, `logoUrl` e `configurationCompleted`.
- **FR-004**: O sistema MUST implementar `GET /api/v1/events/{id}/registration/categories` retornando lista de categorias ativas ou todas.
- **FR-005**: O sistema MUST implementar `GET /api/v1/events/{id}/theme-axes` retornando lista de eixos temáticos.
- **FR-006**: O sistema MUST implementar `GET /api/v1/events/{id}/submission/modalities` retornando lista de modalidades de submissão.
- **FR-007**: O sistema MUST implementar `GET /api/v1/events/{id}/statistics` retornando contagens agregadas de inscrições, submissões e avaliadores.
- **FR-008**: O sistema MUST verificar que o usuário autenticado é organizador, co-organizador ou admin do evento antes de retornar dados — acesso de outro organizador deve retornar 403.
- **FR-009**: O módulo dono dos dados de configuração (categorias, eixos, modalidades) é o `mod-event`; os endpoints MUST pertencer ao mesmo módulo que gerencia o evento.
- **FR-010**: O sistema MUST preservar a Clean Architecture: os novos use cases ficam na camada `application/port/in`, implementados em `application/service`, expostos via adapters em `adapter/in/web`.
- **FR-011**: Testes de integração MUST cobrir: GET evento próprio retorna 200, GET evento alheio retorna 403/404, GET sem autenticação retorna 401, GET evento inexistente retorna 404.

### Key Entities

- **Event** (mod-event): Entidade central com todos os campos de configuração; dono do schema `events`.
- **RegistrationCategory**: Categoria de inscrição vinculada ao evento.
- **ThemeAxis**: Eixo temático do evento para classificação de submissões.
- **SubmissionModality**: Modalidade de submissão (ex: artigo completo, resumo).
- **EventStatistics**: Agregado de contagens calculadas a partir de múltiplos módulos.

## Success Criteria *(obrigatório)*

### Measurable Outcomes

- **SC-001**: `GET /api/v1/events/{id}` retorna 200 com todos os campos de `FR-002` em menos de 200ms.
- **SC-002**: `GET /api/v1/events` passa a incluir `type`, `theme`, `logoUrl` e `configurationCompleted` em todos os itens da resposta.
- **SC-003**: Testes de integração passam para todos os cenários de autorização (organizador próprio, alheio, sem autenticação, admin).
- **SC-004**: O `EventAreaLayoutComponent` carrega sem erro 404 após a implementação dos endpoints.
- **SC-005**: Nenhum endpoint existente tem seu contrato alterado — apenas novos endpoints são adicionados.
