# Research: API de Gerenciamento de Evento

**Feature**: 005-event-manage-api | **Date**: 2026-07-13

## Estado atual do backend

### O que EXISTE no mod-event

| Componente | Status | Detalhe |
|---|---|---|
| `GET /api/v1/events` | Incompleto | Retorna id, name, status, startDate, endDate, location, createdAt -- falta type, theme, logoUrl, configurationCompleted |
| `Event.java` | Incompleto | Entidade tem: name, acronym, description, startDate, endDate, location, organizerId, status. Falta: type, theme, logoUrl, flags de configuracao, datas de inscricao/submissao |
| Tabela `events` | Incompleto | Schema V1 tem as colunas basicas; falta type, theme, logo_url, has_registration, configuration_completed, etc. |
| `EventController.java` | Parcial | So tem GET (listagem); sem GET /{id} |
| Entidades RegistrationCategory, ThemeAxis, SubmissionModality | AUSENTES | Nao existem nem no dominio nem no banco |

### O que precisa ser criado

| Item | Tipo | Modulo |
|---|---|---|
| `GET /api/v1/events/{id}` | Endpoint | mod-event |
| `GET /api/v1/events/{id}/registration/categories` | Endpoint | mod-event |
| `GET /api/v1/events/{id}/theme-axes` | Endpoint | mod-event |
| `GET /api/v1/events/{id}/submission/modalities` | Endpoint | mod-event |
| `GET /api/v1/events/{id}/statistics` | Endpoint | mod-event |
| Novos campos em `Event.java` | Dominio | mod-event |
| `RegistrationCategory.java` | Dominio | mod-event |
| `ThemeAxis.java` | Dominio | mod-event |
| `SubmissionModality.java` | Dominio | mod-event |
| `V8__add_event_config_fields.sql` | Migration | application |
| `V9__create_event_config_tables.sql` | Migration | application |

## Decisoes de design

### Controle de acesso em GET /events/{id}

- **Decisao**: Para MVP, verificar `event.organizerId == userId` (organizador principal) OU `hasRole('ADMIN')`
- **Rationale**: o modulo de co-organizadores no backend ainda nao existe; a verificacao simples de organizador principal e suficiente para MVP
- **Expansao futura**: quando co-organizers forem implementados no backend, a verificacao e expandida

### Formato da resposta -- wrapper { data: ... }

- **Decisao**: `GET /api/v1/events/{id}` retorna `{ data: EventDetailResponse }`
- **Rationale**: o frontend (`events-api.service.ts`) ja espera esse formato: `api.get<{ data: Event }>(...).pipe(map(r => r.data))`
- **Consistencia**: o mesmo padrao e usado em `getCategories`, `getThemeAxes`, etc.

### Tabelas de configuracao no MVP

- `registration_categories`: id, event_id, name, description, type (FREE/PAID), price, active
- `theme_axes`: id, event_id, name, description, active
- `submission_modalities`: id, event_id, name, description, active
- Todas com FK para `events.id` ON DELETE CASCADE

### Estatisticas no MVP

- O endpoint `/statistics` retorna estrutura fixa com zeros para MVP (sem JOINs entre modulos)
- As tabelas `submissions`, `registrations`, `committee_members` existem mas sem FK para `events.id` ainda
- Expansao futura: quando os FKs forem adicionados, as contagens serao reais

## Referencias

- [EventController.java](../../../mod-event/src/main/java/com/econference/event/adapter/in/web/EventController.java)
- [Event.java](../../../mod-event/src/main/java/com/econference/event/domain/Event.java)
- [V1__create_initial_schema.sql](../../../application/src/main/resources/db/migration/V1__create_initial_schema.sql)
- [events-api.service.ts](../../../front-econference/src/app/features/organizer/services/events-api.service.ts)
