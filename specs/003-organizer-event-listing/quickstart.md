# Quickstart: Listagem de Eventos do Organizador

**Feature**: `003-organizer-event-listing`  
**Date**: 2026-07-12

## Pré-requisitos

- Java 21 + Maven instalados
- Docker (para subir banco PostgreSQL opcional; H2 funciona por padrão)
- Backend rodando na porta `8080`

## Rodar os testes da feature

```bash
# A partir de back-econference/
./mvnw test -pl mod-event -Dtest="OrganizerEventServiceTest,EventControllerIT,EventControllerSecurityIT"
```

## Chamar o endpoint manualmente (curl)

### 1. Obter token JWT (login)
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"organizer@example.com","password":"senha123"}' \
  | jq -r '.token')
```

### 2. Listar eventos do organizador autenticado
```bash
# Todos os eventos
curl -s http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer $TOKEN" | jq .

# Filtrar por status
curl -s "http://localhost:8080/api/v1/events?status=PUBLISHED&page=1&limit=10" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Resposta esperada (200 OK)
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "name": "SBBD 2025",
      "status": "PUBLISHED",
      "startDate": "2025-09-15",
      "endDate": "2025-09-18",
      "location": "São Paulo, SP",
      "createdAt": "2024-11-01T10:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 1,
    "totalPages": 1
  }
}
```

### Resposta sem autenticação (401)
```bash
curl -s http://localhost:8080/api/v1/events
# → 401 Unauthorized
```

## Arquivos criados/alterados

| Arquivo | Operação |
|---------|----------|
| `mod-event/.../port/in/GetOrganizerEventsUseCase.java` | Criar |
| `mod-event/.../service/OrganizerEventService.java` | Criar |
| `mod-event/.../port/out/EventRepositoryPort.java` | Estender (+findByOrganizerId paginado) |
| `mod-event/.../adapter/out/persistence/EventRepositoryAdapter.java` | Estender |
| `mod-event/.../repository/EventRepository.java` | Estender |
| `mod-event/.../controller/EventController.java` | Estender (+GET /events) |
| `mod-event/.../dto/response/OrganizerEventsResponse.java` | Criar |
| `mod-event/.../dto/response/EventSummaryResponse.java` | Criar |
| `mod-event/.../test/.../OrganizerEventServiceTest.java` | Criar |
| `mod-event/.../test/.../EventControllerIT.java` | Criar |
| `mod-event/.../test/.../EventControllerSecurityIT.java` | Criar |
