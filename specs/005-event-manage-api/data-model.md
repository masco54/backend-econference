# Data Model: API de Gerenciamento de Evento

**Feature**: 005-event-manage-api | **Date**: 2026-07-13

## Entidades

### Event (ampliada)

Campos a adicionar via migration V8:

| Campo | Tipo SQL | Tipo Java | Observacao |
|---|---|---|---|
| type | VARCHAR(50) | String | ex: CONGRESS, CONFERENCE, WORKSHOP |
| theme | TEXT | String | tema/area do evento |
| logo_url | TEXT | String | URL publica do logo |
| has_registration | BOOLEAN | boolean | inscricoes habilitadas |
| has_submission | BOOLEAN | boolean | submissoes habilitadas |
| has_site | BOOLEAN | boolean | site publico habilitado |
| configuration_completed | BOOLEAN | boolean | checklist concluido |
| registration_start_date | DATE | LocalDate | |
| registration_end_date | DATE | LocalDate | |
| submission_start_date | DATE | LocalDate | |
| submission_end_date | DATE | LocalDate | |

### RegistrationCategory (nova)

\\\
registration_categories (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  type VARCHAR(20) NOT NULL DEFAULT 'FREE',  -- FREE | PAID
  price DECIMAL(10,2),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
)
\\\

### ThemeAxis (nova)

\\\
theme_axes (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
)
\\\

### SubmissionModality (nova)

\\\
submission_modalities (
  id UUID PRIMARY KEY,
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  name VARCHAR(255) NOT NULL,
  description TEXT,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP
)
\\\

## Contracts (API)

Ver [contracts/event-manage.openapi.yaml](contracts/event-manage.openapi.yaml)

## Migrations

- **V8**: Adiciona novos campos em `events`; DEFAULT FALSE/NULL para nao quebrar dados existentes
- **V9**: Cria tabelas `registration_categories`, `theme_axes`, `submission_modalities`

## Migration Ordering Notes

1. Executar `V8__add_event_config_fields.sql` antes de qualquer endpoint novo para garantir compatibilidade da entidade `Event`.
2. Executar `V9__create_event_config_tables.sql` logo apos V8 para habilitar endpoints de categorias, eixos e modalidades.
3. Nao criar schema novo: todas as estruturas permanecem no schema publico com ownership do `mod-event`.
