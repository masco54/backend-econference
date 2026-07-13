# Plano de Implementacao: API de Gerenciamento de Evento

**Branch**: `005-event-manage-api` | **Date**: 2026-07-13 | **Spec**: [spec.md](spec.md)
**Input**: Especificacao da funcionalidade em `/specs/005-event-manage-api/spec.md`

## Summary

O `mod-event` tem atualmente apenas `GET /api/v1/events` (listagem) e nenhum endpoint de gerenciamento individual. A entidade `Event` tambem esta incompleta: faltam campos como `type`, `theme`, `logoUrl`, `hasRegistration`, `configurationCompleted`, etc. O plano implementa: (1) ampliar a entidade `Event` e o schema do banco, (2) `GET /api/v1/events/{id}` com controle de acesso, (3) expandir `EventSummaryResponse`, (4) endpoints de categorias, eixos tematicos, modalidades e estatisticas.

## Technical Context

**Language/Version**: Java 24 / Spring Boot 3.4.4
**Primary Dependencies**: Spring Security 6, Spring Data JPA, Lombok, Jakarta Validation
**Storage**: H2 (dev) / PostgreSQL (prod) -- banco unico, schema publico, tabela `events` com novas colunas; novas tabelas `registration_categories`, `theme_axes`, `submission_modalities`
**Testing**: JUnit 5, Spring Boot Test, MockMvc, @DataJpaTest
**Target Platform**: Servidor JVM em container Linux
**Project Type**: Monolito modular Maven
**Performance Goals**: p95 < 200ms para leituras de evento individual
**Constraints**: Clean Architecture; banco unico; mod-event e dono das entidades Event, RegistrationCategory, ThemeAxis, SubmissionModality
**Architecture Boundaries**: Apenas `mod-event` e alterado; outros modulos recebem dados de evento via contrato publicado existente
**Data Architecture**: Novas colunas em `events` via migration V8; novas tabelas via V9; ownership no schema publico sob responsabilidade do `mod-event`

## Constitution Check

- **Qualidade de Codigo**: Mudancas confinadas ao `mod-event`. Novos use cases, services e adapters seguem o padrao existente (port/in, service, adapter/in/web).
- **Arquitetura Hexagonal**: Domain (Event, RegistrationCategory, etc.) <- Application (GetEventUseCase, etc.) <- Adapter (EventController). Direcao de dependencia preservada.
- **Testes**: Testes de integracao cobrindo autorizacao (proprio/alheio/sem auth), respostas 200/403/404; testes unitarios dos novos services.
- **Consistencia de API**: Novos endpoints seguem o padrao REST existente (URL kebab-case, respostas paginadas onde aplicavel, `{ data: ... }` para recursos individuais).
- **Performance**: Leituras de evento individual com joins controlados; sem N+1 nas listagens.
- **Arquitetura de Dados**: Schema publico; migrations V8 (novos campos em `events`) e V9 (novas tabelas); rollback seguro.
- **Exceptions**: Nenhuma.

## Project Structure

### Documentation (this feature)

\\\	ext
specs/005-event-manage-api/
+-- plan.md
+-- research.md
+-- data-model.md
+-- quickstart.md
+-- contracts/
|   +-- event-manage.openapi.yaml
\\\

### Source Code

\\\	ext
mod-event/src/main/java/com/econference/event/
+-- domain/
|   +-- Event.java                          <- ampliar campos (type, theme, logo, config flags)
|   +-- RegistrationCategory.java          <- NOVO
|   +-- ThemeAxis.java                      <- NOVO
|   +-- SubmissionModality.java             <- NOVO
+-- repository/
|   +-- EventRepository.java               <- novo metodo findByIdAndOrganizerId
|   +-- RegistrationCategoryRepository.java <- NOVO
|   +-- ThemeAxisRepository.java            <- NOVO
|   +-- SubmissionModalityRepository.java   <- NOVO
+-- service/
|   +-- EventService.java                  <- ampliar com getEventForOrganizer()
|   +-- RegistrationCategoryService.java    <- NOVO
|   +-- ThemeAxisService.java               <- NOVO
|   +-- SubmissionModalityService.java      <- NOVO
+-- application/port/in/
|   +-- GetEventDetailUseCase.java          <- NOVO
+-- application/service/
|   +-- OrganizerEventDetailService.java    <- NOVO (implementa GetEventDetailUseCase)
+-- adapter/in/web/
|   +-- EventController.java               <- adicionar GET /{id}; expandir toResponse()
|   +-- EventConfigController.java          <- NOVO (categorias, eixos, modalidades, stats)
+-- adapter/in/web/dto/response/
|   +-- EventSummaryResponse.java          <- adicionar type, theme, logoUrl, configCompleted
|   +-- EventDetailResponse.java            <- NOVO

application/src/main/resources/db/migration/
+-- V8__add_event_config_fields.sql         <- NOVO (type, theme, logo_url, flags em events)
+-- V9__create_event_config_tables.sql      <- NOVO (registration_categories, theme_axes, modalities)
\\\

**Structure Decision**: Monolito modular Maven. Apenas `mod-event` e tocado. Novas entidades e seus repositorios ficam dentro do modulo. `EventController` recebe o endpoint GET /{id}; novo `EventConfigController` agrupa os endpoints de configuracao para manter o controller existente enxuto.

## Impacted Module File Map

- `mod-event/src/main/java/com/econference/event/domain/Event.java`
- `mod-event/src/main/java/com/econference/event/repository/EventRepository.java`
- `mod-event/src/main/java/com/econference/event/application/service/OrganizerEventService.java`
- `mod-event/src/main/java/com/econference/event/adapter/in/web/EventController.java`
- `mod-event/src/main/java/com/econference/event/adapter/in/web/EventConfigController.java`
- `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventSummaryResponse.java`
- `mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventDetailResponse.java`
- `application/src/main/resources/db/migration/V8__add_event_config_fields.sql`
- `application/src/main/resources/db/migration/V9__create_event_config_tables.sql`

## Phase 0: Research

### Descobertas

**Campos faltantes na entidade Event e no banco**
- Decisao: adicionar via migration V8 as colunas: `type VARCHAR(50)`, `theme TEXT`, `logo_url TEXT`, `has_registration BOOLEAN DEFAULT FALSE`, `has_submission BOOLEAN DEFAULT FALSE`, `has_site BOOLEAN DEFAULT FALSE`, `configuration_completed BOOLEAN DEFAULT FALSE`, `registration_start_date DATE`, `registration_end_date DATE`, `submission_start_date DATE`, `submission_end_date DATE`
- Rationale: o frontend espera esses campos tanto na listagem quanto no detalhe do evento
- Alternativa rejeitada: criar tabela separada `event_config` -- desnecessario; esses sao atributos do proprio evento

**Controle de acesso em GET /events/{id}**
- Decisao: verificar se o `organizerId` do evento bate com o usuario autenticado, OU se o usuario e ADMIN, OU se existe um relacionamento de co-organizador (a verificar quando esse modulo existir)
- Rationale: organizadores nao devem ver eventos alheios; a verificacao simples de `organizerId` e suficiente para MVP
- Alternativa futura: quando o modulo de co-organizadores estiver no backend, expandir a verificacao

**Tabelas de configuracao**
- Decisao: criar tabelas `registration_categories`, `theme_axes`, `submission_modalities` vinculadas a `event_id` como FK para `events.id`
- Rationale: o frontend ja espera esses endpoints; serao tabelas simples com name/description/active
- Alternativa rejeitada: usar tabelas ja existentes em outros modulos -- violaria o ownership do mod-event

**Estatisticas do dashboard**
- Decisao: para o MVP, o endpoint `/api/v1/events/{id}/statistics` pode retornar zeros ou contagens simples a partir das tabelas ja existentes (`submissions`, `registrations`, `committee_members`)
- Rationale: as tabelas existem mas nao tem FK para `events.id` ainda; para MVP, retornar estrutura com zeros e suficiente para nao bloquear o frontend
- Alternativa futura: adicionar `event_id` nas tabelas de submissoes/inscricoes/comite quando esses modulos estiverem completos

## Phase 1: Design

Ver [data-model.md](data-model.md), [contracts/event-manage.openapi.yaml](contracts/event-manage.openapi.yaml) e [quickstart.md](quickstart.md).
