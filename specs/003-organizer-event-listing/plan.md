# Plano de Implementação: Listagem de Eventos do Organizador

**Branch**: `003-organizer-event-listing` | **Date**: 2026-07-12 | **Spec**: [spec.md](./spec.md)
**Input**: Especificação da funcionalidade em `/specs/003-organizer-event-listing/spec.md`

## Summary

Expor o endpoint `GET /api/v1/events` no módulo `mod-event` para retornar, de forma paginada, apenas os eventos criados pelo organizador autenticado via JWT. A implementação segue a arquitetura hexagonal existente: port de entrada (`GetOrganizerEventsUseCase`), service de aplicação (`OrganizerEventService`), adapter de persistência reutilizando `EventRepository.findByOrganizerId`. O `EventController` existente (atualmente só com `/health`) recebe o novo handler.

## Technical Context

**Language/Version**: Java 21  
**Primary Dependencies**: Spring Boot 3, Spring Security (JWT via `mod-auth`), Spring Data JPA, Lombok  
**Storage**: H2 (dev/testes locais) / PostgreSQL (produção) — banco físico único, schema `events` de propriedade de `mod-event`  
**Testing**: JUnit 5, Mockito, MockMvc standalone (testes de controller), `@SpringBootTest` ou standalone para unit tests de service  
**Target Platform**: JVM em container Linux (monólito modular)  
**Performance Goals**: p95 < 300 ms para listagem de até 100 eventos em ambiente local  
**Constraints**: Direção de dependências para dentro; domínio sem Spring annotations; nenhum acesso direto ao schema de outro módulo; `EventRepository` já possui `findByOrganizerId(UUID)` — adicionar versão com `Pageable`  
**Scale/Scope**: Apenas `mod-event`; nenhum outro módulo afetado  
**Architecture Boundaries**: Adapter de entrada (`EventController`) → Port (`GetOrganizerEventsUseCase`) → Service (`OrganizerEventService`) → Port de saída (`EventRepositoryPort`) → Adapter de persistência (adapter que já existe, precisará de novo método)  
**Data Architecture**: Tabela `events` no schema de `mod-event`; nenhuma migração necessária (atributos existentes cobrem todos os campos do DTO de resposta)

## Constitution Check

- **Qualidade de Código**: Apenas `mod-event` é afetado. `EventController` (adapter de entrada), novo use case port e service de aplicação, extensão do `EventRepositoryPort` e do adapter de persistência. Nenhuma lógica vaza para `shared` ou `application`.
- **Arquitetura Hexagonal/Clean**: Camada de domínio: `Event`, `EventStatus` (inalterados). Camada de aplicação: novo `GetOrganizerEventsUseCase` (port in) + `OrganizerEventService`. Port de saída: `EventRepositoryPort` recebe método `findByOrganizerId(UUID, Pageable)`. Adapter de entrada: `EventController`. Adapter de saída: `EventRepositoryAdapter`. Direção de dependências: controller → use case port ← service → repository port ← adapter. ✅ Sem violações.
- **Testes**: (1) `OrganizerEventServiceTest` — unit test Mockito cobrindo listagem, lista vazia e filtro por status; (2) `EventControllerIT` — MockMvc standalone cobrindo HTTP 200 com dados, HTTP 200 lista vazia, HTTP 401 sem token; (3) `EventControllerSecurityIT` — WebMvcTest cobrindo 401 sem autenticação, 403 sem role ORGANIZER, 200 com role ORGANIZER.
- **Consistência de UX/API**: Envelope de resposta `{ "data": [...], "pagination": { "page", "limit", "total", "totalPages" } }` idêntico ao padrão de `AdminEventRequestListResponse`. Parâmetro `status` opcional. Nomenclatura segue padrão REST existente.
- **Performance**: Query simples `WHERE organizer_id = ?` com índice implícito em UUID; paginação via `Pageable`. Risco de N+1 inexistente (sem joins). Orçamento de 300 ms amplamente coberto.
- **Arquitetura de Dados**: Schema `events` — propriedade de `mod-event`. Nenhuma migração necessária. Nenhuma interação com schema de outro módulo.
- **Exceptions**: Nenhuma.

## Project Structure

### Documentation (this feature)

```text
specs/003-organizer-event-listing/
├── plan.md              ← este arquivo
├── research.md          ← Fase 0
├── data-model.md        ← Fase 1
├── quickstart.md        ← Fase 1
├── contracts/           ← Fase 1
└── tasks.md             ← /speckit.tasks (não criado aqui)
```

### Source Code (repository root)

```text
mod-event/
├── src/main/java/com/econference/event/
│   ├── application/
│   │   ├── port/
│   │   │   ├── in/
│   │   │   │   └── GetOrganizerEventsUseCase.java          [NEW]
│   │   │   └── out/
│   │   │       └── EventRepositoryPort.java                [EXTEND: +findByOrganizerId paginado]
│   │   └── service/
│   │       └── OrganizerEventService.java                  [NEW]
│   ├── adapter/
│   │   ├── in/web/
│   │   │   ├── EventController.java                        [EXTEND: +GET /events]
│   │   │   └── dto/response/
│   │   │       └── OrganizerEventsResponse.java            [NEW]
│   │   └── out/persistence/
│   │       └── EventRepositoryAdapter.java                 [EXTEND: +findByOrganizerId paginado]
│   └── repository/
│       └── EventRepository.java                            [EXTEND: +findByOrganizerId(UUID, Pageable)]
└── src/test/java/com/econference/event/
    ├── adapter/in/web/
    │   ├── EventControllerIT.java                          [NEW]
    │   └── EventControllerSecurityIT.java                  [NEW]
    └── application/service/
        └── OrganizerEventServiceTest.java                  [NEW]
```

**Structure Decision**: A funcionalidade pertence inteiramente a `mod-event`. O `EventController` já existe no pacote `controller` — será movido ou mantido; o handler novo segue o mesmo pacote. `shared` não é alterado. `application` (composition root) não recebe lógica nova.

## Complexity Tracking

Sem violações de constituição.

---

## Phase 0 — Research

*Todos os itens resolvidos. Ver [research.md](./research.md) para decisões completas.*

| Questão | Decisão |
|---------|---------|
| Como obter o `organizerId` do token? | `@AuthenticationPrincipal(expression = "userId") UUID organizerId` (padrão existente) |
| Qual role autoriza o endpoint? | `@PreAuthorize("hasRole('ORGANIZER')")` |
| Paginação no repositório? | Sobrecarga `findByOrganizerId(UUID, Pageable)` no Spring Data JPA |
| Filtro por status? | `findByOrganizerIdAndStatus(UUID, EventStatus, Pageable)` — service escolhe baseado no param |
| Envelope de resposta? | `{ data: [...], pagination: { page, limit, total, totalPages } }` — padrão existente |
| Migração necessária? | Não |

---

## Phase 1 — Design

*Ver artefatos gerados:*

- [data-model.md](./data-model.md) — entidades, DTO, regras de negócio, diagrama de camadas
- [contracts/organizer-events.openapi.yaml](./contracts/organizer-events.openapi.yaml) — contrato OpenAPI 3.1
- [quickstart.md](./quickstart.md) — como rodar testes e chamar o endpoint

### Constitution Check Pós-Design *(revalidação)*

Todos os itens do Constitution Check inicial foram confirmados após o design:

- ✅ Fronteiras de módulo preservadas — apenas `mod-event` alterado
- ✅ Direção de dependências para dentro — domínio inalterado, sem Spring no service de domínio
- ✅ Banco físico único — tabela `events` já existente, zero migrações
- ✅ Consistência de API — envelope idêntico ao padrão existente
- ✅ Performance — query simples com paginação nativa, sem N+1
- ✅ Testes planejados cobrem unit (service), integração (controller) e segurança (WebMvcTest)
- ✅ Sem violações de constituição

