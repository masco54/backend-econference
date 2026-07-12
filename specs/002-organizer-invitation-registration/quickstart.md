# Quickstart: Cadastro de Organizador via Link de Ativação (Backend)

**Feature**: `002-organizer-activation-registration`  
**Date**: 2026-07-11

## Pré-requisitos

- Java 24, Maven 3.9+
- Docker (para PostgreSQL local, se usar perfil persistente)
- Backend rodando na porta `8080`

## Executar localmente (perfil H2)

```bash
cd back-econference
mvn spring-boot:run -pl application -am
```

O banco H2 em memória será criado automaticamente com as migrations.

## Testar o fluxo completo via cURL

### 1. Simular aprovação de evento (requer token ADMIN)

```bash
# Login como admin
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@econference.com","password":"admin123"}'

# Aprovar uma solicitação de evento existente (ID de exemplo)
curl -b cookies.txt -X POST http://localhost:8080/api/v1/events/requests/{eventRequestId}/approve \
  -H "Content-Type: application/json" \
  -d '{"message":"Solicitação aprovada!"}'
# → O sistema deve gerar automaticamente um OrganizerActivationToken e logar o token no console (modo dev)
```

### 2. Validar token de ativação

```bash
curl http://localhost:8080/api/v1/auth/organizer-activation/validate?token={token}
# Resposta esperada (token válido):
# {
#   "valid": true,
#   "firstName": "Carlos",
#   "lastName": "Mendes",
#   "email": "carlos.mendes@example.com",
#   "phone": "(11) 98765-4321",
#   "eventName": "Conferência Internacional de Engenharia 2026"
# }
```

### 3. Concluir cadastro de organizador

```bash
curl -X POST http://localhost:8080/api/v1/auth/organizer-activation/complete \
  -H "Content-Type: application/json" \
  -d '{
    "token": "{token}",
    "password": "senhaSegura123",
    "phone": "(11) 91234-5678"
  }'
# Resposta esperada:
# HTTP 201
# {
#   "userId": "...",
#   "roles": ["ORGANIZER", "PARTICIPANT"],
#   "eventId": "..."
# }
```

### 4. Login com a conta recém-criada

```bash
curl -c cookies_organizer.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"carlos.mendes@example.com","password":"senhaSegura123"}'
```

## Executar testes unitários do mod-auth

```bash
cd back-econference
mvn test -pl mod-auth
```

## Executar testes unitários do mod-event (verificar wiring do novo port)

```bash
cd back-econference
mvn test -pl mod-event
```

## Executar todos os testes do projeto

```bash
cd back-econference
mvn test
```

## Arquivos-chave a implementar

| Arquivo | Módulo | Tipo |
|---------|--------|------|
| `OrganizerActivationTokenPort.java` | `shared` | Contrato publicado |
| `OrganizerActivationToken.java` | `mod-auth/domain` | Entidade JPA |
| `OrganizerActivationTokenService.java` | `mod-auth/application/service` | Use case: criação + validação |
| `CompleteOrganizerActivationService.java` | `mod-auth/application/service` | Use case: conclusão do cadastro |
| `OrganizerActivationController.java` | `mod-auth/adapter/in/web` | REST controller |
| `OrganizerActivationTokenCreationAdapter.java` | `mod-auth/adapter/out/identity` | Implementa `OrganizerActivationTokenPort` |
| `OrganizerActivationTokenJpaRepository.java` | `mod-auth/adapter/out/persistence` | Spring Data JPA |
| `organizer-activation-email.html` | `mod-auth/resources/templates/email` | Template Thymeleaf |
| `OrganizerActivationTokenEventAdapter.java` | `mod-event/adapter/out/activation` | Adapter de saída que chama o port |
| Migration SQL | `mod-auth/resources/db/migration` | Flyway `V{next}__create_organizer_activation_tokens.sql` |

## Propriedades de configuração

```yaml
# application.yml (ou application-local.yml)
app:
  organizer-activation:
    expiry-days: 7
    base-url: ${APP_BASE_URL:http://localhost:4200}
```

## Notas de Debug

- Em modo dev/H2, o token gerado pode ser logado no nível `DEBUG` no `OrganizerActivationTokenService`
- O envio de e-mail em modo local usa `JavaMailSenderImpl` configurado para porta 1025 (MailHog/Mailpit)
- Para testar token expirado, altere `expiry-days: 0` temporariamente ou manipule `expires_at` via H2 console (`http://localhost:8080/h2-console`)
