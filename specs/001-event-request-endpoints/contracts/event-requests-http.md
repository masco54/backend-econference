# Contract: Event Requests HTTP API

## Canonical Source

- Artefato de referência: `C:\Users\marco\projetos econference\back-econference\event-requests.openapi.yaml`
- Este documento resume a superfície que a implementação deve preservar.

## Public Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/public/event-requests` | Não | Submeter uma nova solicitação pública de evento |
| POST | `/public/event-requests/track` | Não | Consultar status por código e e-mail |
| GET | `/public/event-requests/activate?token=...` | Não | Validar token de ativação |
| POST | `/public/event-requests/activate` | Não | Consumir token e concluir ativação |

## Admin Endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| GET | `/admin/event-requests` | ADMIN | Listar solicitações com paginação e filtros |
| GET | `/admin/event-requests/{requestId}` | ADMIN | Obter detalhe da solicitação |
| POST | `/admin/event-requests/{requestId}/approve` | ADMIN | Aprovar solicitação |
| POST | `/admin/event-requests/{requestId}/reject` | ADMIN | Recusar solicitação com motivo |
| POST | `/admin/event-requests/{requestId}/add-note` | ADMIN | Adicionar nota administrativa |
| POST | `/admin/event-requests/{requestId}/reissue-token` | ADMIN | Reemitir token expirado |

## Core Response Semantics

- `201 Created` na submissão pública bem-sucedida.
- `200 OK` para consultas, decisões administrativas e ativação bem-sucedida.
- `400 Bad Request` para erro de validação, token inválido/expirado ou transição de estado inválida.
- `404 Not Found` para combinação pública inexistente ou recurso administrativo ausente.
- `409 Conflict` para duplicidade de solicitação em aberto.
- `403 Forbidden` para acesso administrativo sem permissão válida.

## Contract Rules

- O acompanhamento público deve usar resposta genérica para combinação inválida de código/e-mail.
- A recusa administrativa exige motivo obrigatório.
- A reemissão de token só é válida para solicitações aprovadas com token expirado ou inválido.
- A ativação só pode acontecer uma vez por token.
- O envelope de erro deve seguir o padrão já adotado pelo backend.
