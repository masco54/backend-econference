# Quickstart: Solicitação de Eventos

## Objetivo

Validar localmente o fluxo de solicitação pública de eventos, revisão administrativa e ativação pós-aprovação no backend.

## Pré-requisitos

- Java 24 instalado
- Maven Wrapper disponível no repositório
- Perfil default com H2 ou profile `postgres` com `docker compose up -d`
- Uma conta administrativa válida para os endpoints `/admin/event-requests/*`

## Subir a aplicação

### H2 local

```powershell
.\mvnw.cmd -pl application spring-boot:run
```

### PostgreSQL local

```powershell
docker compose up -d
.\mvnw.cmd -pl application spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Fluxo principal

### 1. Enviar solicitação pública

```http
POST /public/event-requests
Content-Type: application/json

{
  "requesterFirstName": "Ana",
  "requesterLastName": "Silva",
  "requesterEmail": "ana.silva@example.com",
  "requesterPhone": "+55 11 99999-9999",
  "requesterInstitution": "UFSC",
  "eventName": "Congresso Brasileiro de IA",
  "eventTheme": "Aplicações de IA na pesquisa científica",
  "eventStartDate": "2026-09-10",
  "eventEndDate": "2026-09-12",
  "eventLocation": "Florianópolis",
  "eventType": "conference",
  "hasRegistration": true,
  "hasSubmission": true,
  "hasSite": true
}
```

Esperado: `201 Created` com `id`, `trackingCode` e mensagem de confirmação.

### 2. Consultar andamento

```http
POST /public/event-requests/track
Content-Type: application/json

{
  "trackingCode": "SOL-2026-A3F8",
  "email": "ana.silva@example.com"
}
```

Esperado: `200 OK` com `status`, `statusHistory`, `eventName` e `createdAt`.

### 3. Listar solicitações como admin

Autentique-se pelo fluxo já existente de `mod-auth` e envie o cookie `auth_token`.

```http
GET /admin/event-requests?status=requested&page=1&limit=10
Cookie: auth_token=...
```

Esperado: `200 OK` com paginação e lista filtrada.

### 4. Aprovar uma solicitação

```http
POST /admin/event-requests/{requestId}/approve
Cookie: auth_token=...
Content-Type: application/json

{
  "adminNotes": "Solicitação aderente ao calendário de eventos"
}
```

Esperado: `200 OK`, geração de `linkedEventId`, token de ativação e histórico atualizado.

### 5. Adicionar nota administrativa

```http
POST /admin/event-requests/{requestId}/add-note
Cookie: auth_token=...
Content-Type: application/json

{
  "note": "Precisamos confirmar o número de trilhas e o comitê local."
}
```

Esperado: `200 OK`, atualização de `adminNotes`, histórico incrementado e status em `under_review`.

### 6. Rejeitar uma solicitação

```http
POST /admin/event-requests/{requestId}/reject
Cookie: auth_token=...
Content-Type: application/json

{
  "reason": "O período informado conflita com eventos já homologados pela coordenação."
}
```

Esperado: `200 OK`, status `rejected`, motivo persistido e histórico atualizado.

### 7. Reemitir token expirado

```http
POST /admin/event-requests/{requestId}/reissue-token
Cookie: auth_token=...
```

Esperado: `200 OK` apenas para solicitações aprovadas com token expirado, já utilizado ou ausente, com novo token e nova expiração.

### 8. Validar token de ativação

```http
GET /public/event-requests/activate?token=<uuid>
```

Esperado: `200 OK` enquanto o token estiver válido e não utilizado.

### 9. Concluir ativação

```http
POST /public/event-requests/activate
Content-Type: application/json

{
  "token": "<uuid>",
  "password": "NovaSenhaSegura123",
  "confirmPassword": "NovaSenhaSegura123"
}
```

Esperado: `200 OK` com `eventId`, `userId`, `message` e `userCreated`.

## Testes automatizados esperados

```powershell
.\mvnw.cmd -pl shared,mod-event -am test "-Dtest=EventRequestReviewServiceTest,EventRequestAdminQueryIT,EventRequestAdminControllerIT,EventRequestAdminSecurityIT" "-Dsurefire.failIfNoSpecifiedTests=false"
.\mvnw.cmd -pl application,shared,mod-auth,mod-event -am test "-Dtest=EventModuleArchitectureTest,SubmitEventRequestServiceTest,EventRequestRepositoryAdapterIT,EventRequestPublicSubmissionControllerIT,EventRequestTrackingAndActivationControllerIT,EventRequestActivationServiceTest,EventRequestActivationRepositoryIT,EventRequestAccountProvisioningPortContractTest,EventRequestMigrationIT,EventRequestReviewServiceTest,EventRequestAdminQueryIT,EventRequestAdminControllerIT,EventRequestAdminSecurityIT" "-Dsurefire.failIfNoSpecifiedTests=false"
```

Cobertura mínima esperada:

- regras de duplicidade, datas e transição de estados
- busca por tracking code + email
- paginação e filtros administrativos
- autorização de endpoints admin
- ativação com token válido, expirado e já utilizado
- integração por contrato com `mod-auth`

## Verificações operacionais

- Confirmar que `GET /api/events/health` continua retornando `200`.
- Confirmar que falhas de validação retornam envelope de erro consistente com os demais módulos.
- Confirmar que listagens administrativas não executam consultas em cascata desnecessárias.
- Confirmar que aprovações criam o evento associado com status `PENDING` antes da ativação do organizador.
