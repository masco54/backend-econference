# Data Model: Solicitação de Eventos

## Ownership

- **Módulo dono**: `mod-event`
- **Persistência**: tabelas `event_requests` e `event_request_status_history` migradas via Flyway em `application`
- **Integrações externas ao módulo**: leitura/autorização de identidade e provisionamento de conta via contrato publicado por `mod-auth`

## Entities

### EventRequest

Agregado principal da solicitação pública de evento.

| Campo | Tipo | Obrigatório | Observações |
|------|------|-------------|-------------|
| `id` | UUID | Sim | Herdado de `BaseEntity` |
| `trackingCode` | String | Sim | Código único do tipo `SOL-YYYY-XXXX` |
| `status` | Enum | Sim | `REQUESTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED` |
| `requesterFirstName` | String | Sim | 2-50 caracteres |
| `requesterLastName` | String | Sim | 2-50 caracteres |
| `requesterName` | String | Sim | Derivado ou persistido por compatibilidade |
| `requesterEmail` | String | Sim | E-mail válido |
| `requesterPhone` | String | Não | Telefone opcional |
| `requesterInstitution` | String | Não | Instituição/organização |
| `eventName` | String | Sim | 5-200 caracteres |
| `eventTheme` | String | Sim | 10-500 caracteres |
| `eventStartDate` | LocalDate | Sim | Data de início |
| `eventEndDate` | LocalDate | Sim | Deve ser posterior à inicial |
| `eventLocation` | String | Sim | 5-200 caracteres |
| `eventType` | Enum/String | Sim | Reutiliza catálogo de tipos aceitos pelo produto |
| `hasRegistration` | Boolean | Sim | Capacidade desejada |
| `hasSubmission` | Boolean | Sim | Capacidade desejada |
| `hasSite` | Boolean | Sim | Capacidade desejada |
| `adminNotes` | String | Não | Última nota administrativa relevante |
| `rejectionReason` | String | Não | Obrigatório ao recusar |
| `activationToken` | UUID/String | Não | Gerado na aprovação ou reemissão |
| `activationTokenExpiresAt` | Instant/LocalDateTime | Não | Expiração da ativação |
| `activationTokenUsedAt` | Instant/LocalDateTime | Não | Marca uso único |
| `linkedEventId` | UUID | Não | Evento criado ao aprovar |
| `linkedUserId` | UUID | Não | Usuário vinculado ou provisionado ao ativar |
| `createdAt` | Timestamp | Sim | Herdado de `BaseEntity` |
| `updatedAt` | Timestamp | Sim | Herdado de `BaseEntity` |

### EventRequestStatusHistory

Registro auditável de mudanças de estado e mensagens operacionais.

| Campo | Tipo | Obrigatório | Observações |
|------|------|-------------|-------------|
| `id` | UUID | Sim | Identificador próprio |
| `eventRequestId` | UUID | Sim | FK para `event_requests` |
| `status` | Enum | Sim | Estado assumido no momento |
| `message` | String | Não | Nota, motivo de recusa ou comentário |
| `actorUserId` | UUID | Não | Usuário admin quando aplicável |
| `createdAt` | Timestamp | Sim | Ordenação histórica |

### Linked Event

Representa o `Event` já existente no módulo, criado quando a solicitação é aprovada.

| Campo | Tipo | Papel na feature |
|------|------|-------------------|
| `id` | UUID | Referenciado como `linkedEventId` |
| `organizerId` | UUID | Atualizado ou confirmado na ativação |
| `status` | EventStatus | Inicializado em estado apropriado para pós-aprovação |

### Account Provisioning Result

Resultado do contrato com `mod-auth` durante a ativação.

| Campo | Tipo | Papel na feature |
|------|------|-------------------|
| `userId` | UUID | Conta vinculada à solicitação |
| `userCreated` | Boolean | Indica se houve provisionamento |
| `roles` | Collection<String> | Confirma elegibilidade como organizador |

## Relationships

- `EventRequest` 1:N `EventRequestStatusHistory`
- `EventRequest` 0:1 `Event`
- `EventRequest` 0:1 `User` via contrato de `mod-auth`

## State Machine

```text
REQUESTED
  -> UNDER_REVIEW   (admin adiciona nota ou inicia análise)
  -> APPROVED       (admin aprova diretamente)
  -> REJECTED       (admin recusa com motivo)

UNDER_REVIEW
  -> APPROVED       (admin aprova)
  -> REJECTED       (admin recusa com motivo)

APPROVED
  -> APPROVED       (reemissão de token sem mudar estado)

REJECTED
  -> terminal
```

## Invariants

1. `eventEndDate` deve ser maior ou igual a `eventStartDate`, com rejeição quando a ordem for inválida.
2. Não pode existir mais de uma solicitação com mesmo `requesterEmail` e `eventName` em estado `REQUESTED` ou `UNDER_REVIEW`.
3. `rejectionReason` é obrigatório em toda transição para `REJECTED`.
4. `linkedEventId` deve existir ao final de uma aprovação bem-sucedida.
5. `linkedUserId` só pode ser preenchido após ativação bem-sucedida.
6. `activationToken` não pode ser reutilizado após `activationTokenUsedAt` ser preenchido.
7. Toda mudança de estado e toda nota administrativa relevante deve gerar um item em `EventRequestStatusHistory`.

## Persistence Notes

- Índice único composto para prevenção de duplicidade em solicitações abertas.
- Índice por `tracking_code` para busca pública.
- Índice por `status` e `created_at desc` para fila administrativa.
- Índice por `requester_email` para apoio à busca administrativa e à validação de acompanhamento.
- O histórico deve ser carregado de forma controlada para evitar N+1 em listagens administrativas.

## Validation Rules

- Nome e sobrenome do solicitante: 2 a 50 caracteres.
- Nome do evento: 5 a 200 caracteres.
- Tema/descrição: 10 a 500 caracteres.
- Local do evento: 5 a 200 caracteres.
- E-mail em formato válido.
- Motivo de recusa com tamanho mínimo legível.
- Token de ativação com expiração configurável e uso único.
