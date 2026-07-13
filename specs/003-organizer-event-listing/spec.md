# Especificação de Funcionalidade: Listagem de Eventos do Organizador (Back-end)

**Feature Branch**: `003-organizer-event-listing`  
**Created**: 2026-07-12  
**Status**: Draft  
**Input**: Descrição do usuário: "quando organizador loga no sistema, preciso que seja listados os eventos que ele criou. Endpoint de listagem de eventos do organizador."

## User Scenarios & Testing *(obrigatório)*

### User Story 1 - Listar eventos criados pelo organizador autenticado (Priority: P1)

Quando um organizador autenticado chama o endpoint de listagem de eventos, o sistema retorna apenas os eventos que ele criou, com as informações essenciais para exibição no frontend.

**Why this priority**: É a funcionalidade central. Sem este endpoint, o frontend não consegue exibir os eventos do organizador com dados reais.

**Independent Test**: Pode ser testado chamando `GET /api/v1/events` com token de um organizador que possui eventos cadastrados e verificando que a resposta contém apenas eventos com `organizerId` igual ao ID do usuário autenticado.

**Acceptance Scenarios**:

1. **Given** um organizador autenticado com dois eventos cadastrados, **When** faz `GET /api/v1/events`, **Then** o sistema retorna HTTP 200 com uma lista contendo exatamente esses dois eventos e metadados de paginação.
2. **Given** um organizador autenticado sem nenhum evento cadastrado, **When** faz `GET /api/v1/events`, **Then** o sistema retorna HTTP 200 com lista vazia e `total: 0`.
3. **Given** dois organizadores distintos com eventos diferentes, **When** cada um chama `GET /api/v1/events`, **Then** cada resposta contém apenas os eventos criados por aquele organizador, sem vazamento de dados entre organizadores.

---

### User Story 2 - Acesso negado para usuário não autenticado (Priority: P2)

Quando um usuário não autenticado (sem token válido) tenta acessar a listagem de eventos do organizador, o sistema nega o acesso.

**Why this priority**: Garante que dados dos organizadores não sejam expostos sem autenticação; é requisito de segurança básico.

**Independent Test**: Pode ser testado chamando `GET /api/v1/events` sem header de autorização e verificando que a resposta é HTTP 401.

**Acceptance Scenarios**:

1. **Given** uma requisição sem token de autenticação, **When** faz `GET /api/v1/events`, **Then** o sistema retorna HTTP 401 com mensagem padronizada de não autorizado.
2. **Given** uma requisição com token expirado ou inválido, **When** faz `GET /api/v1/events`, **Then** o sistema retorna HTTP 401.

---

### Edge Cases

- O que acontece quando o organizador possui um número muito grande de eventos? → A resposta deve incluir paginação; o endpoint deve aceitar parâmetros `page` e `limit`.
- Como o sistema lida com falha de autenticação/autorização? → Retorna HTTP 401 com corpo padronizado.
- O que acontece quando o banco de dados está indisponível? → O sistema deve retornar HTTP 500 com mensagem de erro genérica sem expor detalhes internos.
- O que acontece se o parâmetro de filtro receber um valor inválido (ex.: status inexistente)? → O sistema deve retornar HTTP 400 com mensagem descritiva.

## Requirements *(obrigatório)*

### Functional Requirements

- **FR-001**: O sistema DEVE expor o endpoint `GET /api/v1/events` para usuários autenticados com papel de organizador.
- **FR-002**: O endpoint DEVE retornar apenas os eventos cujo `organizerId` corresponde ao ID do usuário autenticado.
- **FR-003**: A resposta DEVE incluir, para cada evento: identificador, nome, status, data de início, data de fim, localização e data de criação.
- **FR-004**: A resposta DEVE seguir o envelope de paginação padrão do sistema com campos `data` (lista de eventos) e `pagination` (total, page, limit).
- **FR-005**: O endpoint DEVE aceitar o parâmetro de consulta `status` para filtrar eventos por status (`PENDING`, `DRAFT`, `PUBLISHED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`).
- **FR-006**: O endpoint DEVE retornar HTTP 401 para requisições sem autenticação válida.
- **FR-007**: O endpoint DEVE retornar HTTP 400 com mensagem descritiva para parâmetros inválidos.
- **FR-008**: O módulo `mod-event` é dono do caso de uso, do domínio e do schema de persistência; nenhuma lógica de negócio deve vazar para `shared` ou para outros módulos.
- **FR-009**: A implementação DEVE seguir a arquitetura hexagonal (ports & adapters) existente no módulo: caso de uso via port de entrada, repositório via port de saída.
- **FR-010**: O sistema DEVE incluir testes unitários cobrindo o caso de uso de listagem e testes de integração/contrato cobrindo o controller.

### Key Entities

- **Event**: Entidade principal do módulo `mod-event` — contém `id`, `name`, `description`, `startDate`, `endDate`, `location`, `organizerId`, `status`.
- **EventStatus**: Enumeração dos possíveis status de um evento — `PENDING`, `DRAFT`, `PUBLISHED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`.
- **EventSummaryResponse** (DTO de saída): Projeção de `Event` para a resposta da listagem — sem dados sensíveis internos.

## Success Criteria *(obrigatório)*

### Measurable Outcomes

- **SC-001**: O endpoint `GET /api/v1/events` retorna HTTP 200 com a lista correta de eventos para qualquer organizador autenticado com pelo menos um evento cadastrado.
- **SC-002**: O endpoint responde em até 500ms para organizadores com até 100 eventos em ambiente local.
- **SC-003**: Requisições sem token válido recebem HTTP 401 em 100% dos casos.
- **SC-004**: Testes unitários do caso de uso e testes de integração do controller passam sem falhas.
- **SC-005**: Nenhum evento de outro organizador é retornado na listagem — isolamento de dados verificado por testes automatizados.

## Assumptions

- O módulo `mod-auth` já provê o mecanismo de autenticação JWT; o `mod-event` extrai o ID do organizador do token via Spring Security sem depender da implementação interna de `mod-auth`.
- O banco físico único é compartilhado; o schema da tabela `events` pertence ao `mod-event`.
- O `EventRepository` já possui `findByOrganizerId(UUID organizerId)`, que pode ser reutilizado ou estendido com paginação.
- A entidade `Event` não precisa de novos atributos para atender esta spec; apenas o endpoint e as camadas de aplicação precisam ser criados.
- H2 em memória é suficiente para testes de integração locais desta funcionalidade.
