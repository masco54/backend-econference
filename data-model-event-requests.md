# Data Model: Solicitação Pública de Evento (sem cadastro)

**Feature Branch**: `022-public-event-request`  
**Date**: 2026-03-12

## Entities

### EventRequest (Nova Entidade)

Solicitação de evento criada por um visitante não autenticado.

| Campo                      | Tipo                 | Obrigatório | Descrição                                           |
| -------------------------- | -------------------- | ----------- | --------------------------------------------------- |
| `id`                       | `string`             | Sim         | UUID da solicitação                                 |
| `trackingCode`             | `string`             | Sim         | Código de acompanhamento (formato: `SOL-YYYY-XXXX`) |
| `status`                   | `EventRequestStatus` | Sim         | Status do ciclo de vida                             |
| `requesterFirstName`       | `string`             | Sim         | Nome do solicitante                                 |
| `requesterLastName`        | `string`             | Sim         | Sobrenome do solicitante                            |
| `requesterName`            | `string`             | Sim         | Nome completo do solicitante                        |
| `requesterEmail`           | `string`             | Sim         | E-mail do solicitante                               |
| `requesterPhone`           | `string`             | Não         | Telefone do solicitante                             |
| `requesterInstitution`     | `string`             | Não         | Instituição/organização                             |
| `eventName`                | `string`             | Sim         | Nome do evento proposto                             |
| `eventTheme`               | `string`             | Sim         | Tema/descrição do evento                            |
| `eventStartDate`           | `string`             | Sim         | Data de início (ISO 8601)                           |
| `eventEndDate`             | `string`             | Sim         | Data de término (ISO 8601)                          |
| `eventLocation`            | `string`             | Sim         | Local do evento                                     |
| `eventType`                | `EventType`          | Sim         | Tipo do evento (Congress, Conference, etc.)         |
| `hasRegistration`          | `boolean`            | Sim         | Funcionalidade de inscrições desejada               |
| `hasSubmission`            | `boolean`            | Sim         | Funcionalidade de submissões desejada               |
| `hasSite`                  | `boolean`            | Sim         | Funcionalidade de site desejada                     |
| `adminNotes`               | `string`             | Não         | Notas/comentários do administrador                  |
| `rejectionReason`          | `string`             | Não         | Motivo da recusa (obrigatório ao recusar)           |
| `activationToken`          | `string`             | Não         | Token UUID de ativação (gerado na aprovação)        |
| `activationTokenExpiresAt` | `string`             | Não         | Data de expiração do token (ISO 8601)               |
| `activationTokenUsed`      | `boolean`            | Sim         | Se o token já foi utilizado (default: false)        |
| `linkedEventId`            | `string`             | Não         | ID do evento criado após aprovação                  |
| `linkedUserId`             | `string`             | Não         | ID do usuário vinculado após ativação               |
| `createdAt`                | `string`             | Sim         | Data de criação (ISO 8601)                          |
| `updatedAt`                | `string`             | Sim         | Data da última atualização (ISO 8601)               |

### EventRequestStatus (Enum)

```typescript
enum EventRequestStatus {
  REQUESTED = 'requested', // Recebida — solicitação enviada pelo visitante
  UNDER_REVIEW = 'under_review', // Em Análise — admin está avaliando
  APPROVED = 'approved', // Aprovada — evento criado, token gerado
  REJECTED = 'rejected', // Recusada — admin recusou com motivo
}
```

### StatusHistoryEntry (Embutido em EventRequest)

Histórico de mudanças de status para exibição na página de acompanhamento.

| Campo       | Tipo                 | Obrigatório | Descrição                                  |
| ----------- | -------------------- | ----------- | ------------------------------------------ |
| `status`    | `EventRequestStatus` | Sim         | Status naquele momento                     |
| `timestamp` | `string`             | Sim         | Data da mudança (ISO 8601)                 |
| `message`   | `string`             | Não         | Mensagem associada (nota do admin, motivo) |

**Campo adicional em EventRequest:**

| Campo           | Tipo                   | Obrigatório | Descrição                    |
| --------------- | ---------------------- | ----------- | ---------------------------- |
| `statusHistory` | `StatusHistoryEntry[]` | Sim         | Histórico ordenado de status |

### SubmitEventRequestRequest (Request DTO)

Payload enviado pelo visitante ao submeter o formulário.

| Campo                  | Tipo        | Obrigatório | Descrição                                           |
| ---------------------- | ----------- | ----------- | --------------------------------------------------- |
| `requesterFirstName`   | `string`    | Sim         | Nome                                                |
| `requesterLastName`    | `string`    | Sim         | Sobrenome                                           |
| `requesterName`        | `string`    | Não         | Nome completo derivado, mantido por compatibilidade |
| `requesterEmail`       | `string`    | Sim         | E-mail                                              |
| `requesterPhone`       | `string`    | Não         | Telefone                                            |
| `requesterInstitution` | `string`    | Não         | Instituição                                         |
| `eventName`            | `string`    | Sim         | Nome do evento                                      |
| `eventTheme`           | `string`    | Sim         | Tema                                                |
| `eventStartDate`       | `string`    | Sim         | Data início                                         |
| `eventEndDate`         | `string`    | Sim         | Data término                                        |
| `eventLocation`        | `string`    | Sim         | Local                                               |
| `eventType`            | `EventType` | Sim         | Tipo                                                |
| `hasRegistration`      | `boolean`   | Sim         | Inscrições                                          |
| `hasSubmission`        | `boolean`   | Sim         | Submissões                                          |
| `hasSite`              | `boolean`   | Sim         | Site                                                |

### SubmitEventRequestResponse (Response DTO)

Resposta retornada após submissão bem-sucedida.

| Campo          | Tipo     | Descrição                  |
| -------------- | -------- | -------------------------- |
| `id`           | `string` | UUID da solicitação criada |
| `trackingCode` | `string` | Código de acompanhamento   |
| `message`      | `string` | Mensagem de confirmação    |

### TrackEventRequestRequest (Request DTO)

Payload enviado pelo visitante para consultar status.

| Campo          | Tipo     | Obrigatório | Descrição                |
| -------------- | -------- | ----------- | ------------------------ |
| `trackingCode` | `string` | Sim         | Código de acompanhamento |
| `email`        | `string` | Sim         | E-mail do solicitante    |

### TrackEventRequestResponse (Response DTO)

Dados retornados na consulta de status.

| Campo           | Tipo                   | Descrição                                            |
| --------------- | ---------------------- | ---------------------------------------------------- |
| `trackingCode`  | `string`               | Código de acompanhamento                             |
| `status`        | `EventRequestStatus`   | Status atual                                         |
| `eventName`     | `string`               | Nome do evento solicitado                            |
| `statusHistory` | `StatusHistoryEntry[]` | Histórico de mudanças                                |
| `activationUrl` | `string \| null`       | URL de ativação (somente se aprovada e token válido) |
| `createdAt`     | `string`               | Data de criação                                      |

### AdminEventRequestListParams (Filtros Admin)

| Campo    | Tipo                 | Obrigatório | Descrição                               |
| -------- | -------------------- | ----------- | --------------------------------------- |
| `status` | `EventRequestStatus` | Não         | Filtrar por status                      |
| `search` | `string`             | Não         | Busca por nome do solicitante ou evento |
| `page`   | `number`             | Não         | Página (default: 1)                     |
| `limit`  | `number`             | Não         | Itens por página (default: 10)          |

### AdminEventRequestListResponse (Resposta Paginada)

| Campo        | Tipo             | Descrição                |
| ------------ | ---------------- | ------------------------ |
| `data`       | `EventRequest[]` | Lista de solicitações    |
| `pagination` | `PaginationInfo` | Informações de paginação |

### ActivateEventRequestRequest (Ativação)

| Campo             | Tipo     | Obrigatório | Descrição                                 |
| ----------------- | -------- | ----------- | ----------------------------------------- |
| `token`           | `string` | Sim         | Token de ativação                         |
| `password`        | `string` | Condicional | Senha (obrigatório se usuário não existe) |
| `confirmPassword` | `string` | Condicional | Confirmação de senha                      |

### ActivateEventRequestResponse (Resposta de Ativação)

| Campo         | Tipo      | Descrição                      |
| ------------- | --------- | ------------------------------ |
| `eventId`     | `string`  | ID do evento criado            |
| `userId`      | `string`  | ID do usuário vinculado/criado |
| `message`     | `string`  | Mensagem de sucesso            |
| `userCreated` | `boolean` | Se uma nova conta foi criada   |

## Relationships

```
EventRequest 0..1:1 Event     — Uma solicitação aprovada vincula a um evento criado
EventRequest 0..1:1 User      — Uma solicitação ativada vincula a um usuário
EventRequest N:0 EventType    — Usa o enum de tipo de evento existente
```

## Validation Rules

1. **E-mail válido**: `requesterEmail` deve ser um endereço de e-mail com formato válido.
2. **Datas coerentes**: `eventEndDate` deve ser posterior a `eventStartDate`.
3. **Comprimentos**: `eventName` (5-200 chars), `eventTheme` (10-500 chars), `eventLocation` (5-200 chars), `requesterFirstName` e `requesterLastName` (2-50 chars cada), `requesterName` derivado (3-100 chars).
4. **Duplicidade**: Não pode existir duas solicitações com mesmo `(requesterEmail, eventName)` em status `REQUESTED` ou `UNDER_REVIEW`.
5. **Recusa obriga motivo**: `rejectionReason` é obrigatório quando status muda para `REJECTED`.
6. **Token de ativação**: Gerado como UUID criptográfico, válido por 7 dias, invalidado após primeiro uso.
7. **Código de acompanhamento**: Formato `SOL-YYYY-XXXX` onde YYYY é o ano e XXXX são 4 caracteres alfanuméricos uppercase.

## State Transitions

```
                 ┌────────────┐
    Visitante    │            │
    submete  --> │ REQUESTED  │
                 │ (Recebida) │
                 └──────┬─────┘
                        │
                        │ Admin analisa
                        ▼
                 ┌──────────────┐
                 │              │
                 │ UNDER_REVIEW │
                 │ (Em Análise) │
                 └──────┬───────┘
                        │
              ┌─────────┼─────────┐
              │                   │
              ▼                   ▼
      ┌───────────┐       ┌───────────┐
      │           │       │           │
      │ APPROVED  │       │ REJECTED  │
      │ (Aprovada)│       │ (Recusada)│
      │           │       │           │
      └─────┬─────┘       └───────────┘
            │
            │ Token gerado, evento criado
            │ Solicitante ativa conta
            ▼
      ┌───────────────────────┐
      │ Evento status: PENDING│
      │ User: ORGANIZER role  │
      └───────────────────────┘
```

**Nota**: A transição `REQUESTED → APPROVED` é permitida diretamente (sem passar por `UNDER_REVIEW`) quando o admin aprova imediatamente. `UNDER_REVIEW` é opcional para quando o admin quer solicitar mais informações.

## Mock Storage

**localStorage key**: `econference.mock.event-requests-data.v1`  
**localStorage key (tokens)**: `econference.mock.activation-tokens-data.v1`
