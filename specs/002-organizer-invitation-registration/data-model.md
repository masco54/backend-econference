# Data Model: Cadastro de Organizador via Link de Ativação

**Feature**: `002-organizer-activation-registration`  
**Date**: 2026-07-11

## Entidade: OrganizerActivationToken

**Módulo dono**: `mod-auth`  
**Schema**: `mod-auth` (mesmo banco físico único)  
**Tabela**: `organizer_activation_tokens`

### Campos

| Campo | Tipo Java | Coluna SQL | Nullable | Notas |
|-------|-----------|------------|----------|-------|
| `id` | `UUID` (BaseEntity) | `id` UUID PK | NO | Gerado pelo JPA |
| `token` | `String` | `token` VARCHAR(36) UNIQUE | NO | UUID único gerado no momento da criação |
| `firstName` | `String` | `first_name` VARCHAR(50) | NO | Do solicitante original do evento |
| `lastName` | `String` | `last_name` VARCHAR(50) | NO | Do solicitante original do evento |
| `email` | `String` | `email` VARCHAR(255) | NO | E-mail do solicitante; não alterável |
| `phone` | `String` | `phone` VARCHAR(20) | YES | Telefone da solicitação; pode ser sobrescrito pelo usuário |
| `eventName` | `String` | `event_name` VARCHAR(255) | NO | Nome do evento aprovado (para exibição no e-mail e no formulário) |
| `eventId` | `UUID` | `event_id` UUID | NO | Referência lógica ao evento aprovado (não FK — módulo diferente) |
| `eventRequestId` | `UUID` | `event_request_id` UUID | NO | Referência lógica à solicitação aprovada |
| `expiresAt` | `Instant` | `expires_at` TIMESTAMPTZ | NO | `now() + 7 days` (configurável) |
| `usedAt` | `Instant` | `used_at` TIMESTAMPTZ | YES | Null enquanto disponível; preenchido ao concluir o cadastro |
| `createdAt` | `Instant` (BaseEntity) | `created_at` TIMESTAMPTZ | NO | Gerado pelo JPA |

### Índices

```sql
CREATE UNIQUE INDEX idx_organizer_activation_tokens_token ON organizer_activation_tokens(token);
CREATE INDEX idx_organizer_activation_tokens_email ON organizer_activation_tokens(email);
CREATE INDEX idx_organizer_activation_tokens_event_request_id ON organizer_activation_tokens(event_request_id);
```

### Regras de Domínio

- **Token válido**: `usedAt IS NULL AND expiresAt > NOW()`
- **Token expirado**: `expiresAt <= NOW()`
- **Token usado**: `usedAt IS NOT NULL`
- Um token é consumido (usedAt preenchido) na mesma transação em que a conta é criada com sucesso
- `email` não pode ser alterado após a criação do token
- `eventId` e `eventRequestId` são referências lógicas (sem FK para preservar fronteiras de módulo)

---

## Contrato Publicado: OrganizerActivationTokenPort

**Módulo**: `shared/contract`

```java
public interface OrganizerActivationTokenPort {
    OrganizerActivationTokenResult createActivationToken(CreateOrganizerActivationTokenCommand command);

    record CreateOrganizerActivationTokenCommand(
        UUID eventRequestId,
        UUID eventId,
        String firstName,
        String lastName,
        String email,
        String phone,        // nullable
        String eventName
    ) {}

    record OrganizerActivationTokenResult(String token, Instant expiresAt) {}
}
```

---

## DTOs de Entrada e Saída (mod-auth)

### ValidateOrganizerActivationTokenResponse

```java
public record ValidateOrganizerActivationTokenResponse(
    boolean valid,
    String reason,      // "expired" | "already-used" | "not-found" | null
    String firstName,   // null se invalid
    String lastName,    // null se invalid
    String email,       // null se invalid
    String phone,       // null se invalid ou não informado
    String eventName    // null se invalid
) {}
```

### CompleteOrganizerActivationRequest

```java
public record CompleteOrganizerActivationRequest(
    @NotBlank String token,
    @NotBlank @Size(min = 8, max = 100) String password,
    @Pattern(regexp = "^$|^\\+?[1-9]\\d{10,14}$") String phone  // nullable/optional
) {}
```

### CompleteOrganizerActivationResponse

```java
public record CompleteOrganizerActivationResponse(
    UUID userId,
    Set<String> roles,
    UUID eventId
) {}
```

---

## Transições de Estado do Token

```
CRIADO (usedAt=null, expiresAt > now)
    │
    ├─ expiresAt <= now → EXPIRADO (somente leitura, não muda estado no banco)
    │
    └─ POST /complete com sucesso → CONSUMIDO (usedAt = now())
```

---

## Impacto em User (entidade existente)

Ao concluir o cadastro via token:
- `emailVerified = true` (sem envio de e-mail de verificação)
- `roles = {ORGANIZER, PARTICIPANT}`
- `phone` = valor do formulário (sobrepõe o do token se fornecido)
- Nenhuma outra alteração na entidade `User`

---

## Migração SQL (Flyway/Liquibase)

```sql
-- V{next}__create_organizer_activation_tokens.sql
CREATE TABLE organizer_activation_tokens (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    token              VARCHAR(36)  NOT NULL,
    first_name         VARCHAR(50)  NOT NULL,
    last_name          VARCHAR(50)  NOT NULL,
    email              VARCHAR(255) NOT NULL,
    phone              VARCHAR(20),
    event_name         VARCHAR(255) NOT NULL,
    event_id           UUID         NOT NULL,
    event_request_id   UUID         NOT NULL,
    expires_at         TIMESTAMPTZ  NOT NULL,
    used_at            TIMESTAMPTZ,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_organizer_activation_tokens_token UNIQUE (token)
);

CREATE INDEX idx_organizer_activation_tokens_email         ON organizer_activation_tokens(email);
CREATE INDEX idx_organizer_activation_tokens_event_req_id  ON organizer_activation_tokens(event_request_id);
```

**Rollback**: `DROP TABLE organizer_activation_tokens;`  
**Compatibilidade H2**: `gen_random_uuid()` requer H2 2.x (já usado pelo projeto).  
**Compatibilidade PostgreSQL**: nativa.
