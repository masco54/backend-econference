# Data Model: Listagem de Eventos do Organizador

**Feature**: `003-organizer-event-listing`  
**Date**: 2026-07-12

---

## Entidades Afetadas

### Event *(existente — sem alterações)*

| Campo        | Tipo        | Restrições              | Notas                                          |
|--------------|-------------|-------------------------|------------------------------------------------|
| id           | UUID        | PK, NOT NULL            | Herdado de `BaseEntity`                        |
| name         | String      | NOT NULL                |                                                |
| acronym      | String      | max 20                  |                                                |
| description  | TEXT        | nullable                |                                                |
| startDate    | LocalDate   | nullable                |                                                |
| endDate      | LocalDate   | nullable                |                                                |
| location     | String      | nullable                |                                                |
| organizerId  | UUID        | NOT NULL, FK implícita  | ID do usuário criador; pertence a `mod-auth`   |
| status       | EventStatus | NOT NULL, default DRAFT |                                                |
| createdAt    | Instant     | NOT NULL                | Herdado de `BaseEntity`                        |

### EventStatus *(existente — sem alterações)*

```
PENDING | DRAFT | PUBLISHED | IN_PROGRESS | FINISHED | CANCELLED
```

---

## DTO de Saída

### EventSummaryResponse *(novo — adapter layer)*

```java
record EventSummaryResponse(
    UUID   id,
    String name,
    String status,      // serializado como string lowercase
    String startDate,   // ISO-8601 date (yyyy-MM-dd)
    String endDate,
    String location,
    String createdAt    // ISO-8601 instant
)
```

### OrganizerEventsResponse *(novo — adapter layer)*

```java
record OrganizerEventsResponse(
    List<EventSummaryResponse> data,
    PaginationResponse         pagination
) {
    record PaginationResponse(
        int  page,
        int  limit,
        long total,
        int  totalPages
    ) {}
}
```

---

## Regras de Negócio

| Regra | Descrição |
|-------|-----------|
| BR-001 | Apenas eventos cujo `organizerId == ID do usuário autenticado` são retornados |
| BR-002 | Sem parâmetro `status`, todos os status são retornados |
| BR-003 | Com parâmetro `status` inválido (não pertence ao enum), retornar HTTP 400 |
| BR-004 | Paginação padrão: `page=1`, `limit=20` quando não informados |
| BR-005 | `totalPages = ceil(total / limit)` |

---

## Sem Migrações

A tabela `events` e todos os campos necessários já existem. Nenhuma alteração de schema é necessária.

---

## Diagrama de Dependências (Camadas)

```
HTTP Request
    │
    ▼
EventController (adapter/in/web)
    │ @AuthenticationPrincipal → organizerId
    │ @RequestParam → status, page, limit
    ▼
GetOrganizerEventsUseCase (port/in)
    ▲
    │ implements
OrganizerEventService (application/service)
    │
    ▼
EventRepositoryPort (port/out)
    ▲
    │ implements
EventRepositoryAdapter (adapter/out/persistence)
    │
    ▼
EventRepository (Spring Data JPA)
    │
    ▼
Table: events (schema: mod-event)
```
