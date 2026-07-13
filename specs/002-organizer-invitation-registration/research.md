# Research: Cadastro de Organizador via Link de Ativação após Aprovação de Evento

**Feature**: `002-organizer-activation-registration`  
**Date**: 2026-07-11

## Decisão 1: Onde vive o token de ativação — mod-auth vs mod-event

**Decisão**: O token de ativação de organizador (`OrganizerActivationToken`) vive em `mod-auth`.

**Racional**: A responsabilidade de emitir, validar e consumir tokens de autenticação é do `mod-auth`. O `mod-event` já delega o provisionamento de contas para `mod-auth` via `EventRequestAccountProvisioningPort` — o padrão inverso (mod-event aciona mod-auth) está consolidado. Mover o token para `mod-auth` deixa `mod-event` sem dependência de detalhes de autenticação.

**Alternativas consideradas**:
- Estender `ValidateActivationTokenResponse` em `mod-event` para incluir firstName/lastName/phone: **Rejeitado** — manteria lógica de criação de conta (senha, roles, emailVerified) em `mod-event`, violando responsabilidade única. O token existente em `mod-event` tem escopo diferente (rastreamento da solicitação de evento).
- Novo serviço externo de tokens: **Rejeitado** — desnecessário para um monólito modular; aumentaria complexidade operacional.

---

## Decisão 2: Contrato entre mod-event e mod-auth para criação do token

**Decisão**: Novo contrato `OrganizerActivationTokenPort` em `shared/contract/`, seguindo exatamente o padrão de `EventRequestAccountProvisioningPort`.

```java
// shared/contract/OrganizerActivationTokenPort.java
public interface OrganizerActivationTokenPort {
    OrganizerActivationTokenResult createActivationToken(CreateOrganizerActivationTokenCommand command);

    record CreateOrganizerActivationTokenCommand(
        UUID eventRequestId,
        UUID eventId,
        String firstName,
        String lastName,
        String email,
        String phone, // nullable
        String eventName
    ) {}

    record OrganizerActivationTokenResult(String token, Instant expiresAt) {}
}
```

**Racional**: Padrão já estabelecido e compreendido pelo time. `mod-event` depende apenas da interface em `shared`, não de classes internas de `mod-auth`. O `application/` faz o wiring via `@Bean`.

**Alternativas consideradas**:
- Evento de domínio assíncrono (Spring Application Events): **Rejeitado** — adiciona complexidade de mensageria para uma operação síncrona simples. O wiring direto via port é suficiente e mais fácil de testar.
- REST interno (mod-event chama API do mod-auth): **Rejeitado** — acoplamento por HTTP dentro do mesmo processo é anti-padrão para monólito modular.

---

## Decisão 3: Endpoint de validação do token — mod-auth vs mod-event

**Decisão**: Novos endpoints em `mod-auth`: `GET /api/v1/auth/organizer-activation/validate` e `POST /api/v1/auth/organizer-activation/complete`.

**Racional**: O token é gerenciado por `mod-auth`, portanto os endpoints de leitura e escrita desse token pertencem a `mod-auth`. A validação retorna firstName, lastName, email, phone, eventName — dados que `mod-auth` armazenou no token. O frontend não precisa consultar `mod-event` para obter dados do formulário.

**Alternativas consideradas**:
- Manter token em `mod-event` e adicionar campos no `ValidateActivationTokenResponse` existente: **Rejeitado** — concentraria lógica de cadastro (senha, roles) em `mod-event`. Separação de responsabilidades violada.

---

## Decisão 4: Como o mod-event é modificado — onde chamar OrganizerActivationTokenPort

**Decisão**: `ApproveEventRequestService` (em `mod-event/application/service`) recebe um novo port de saída `OrganizerActivationTokenPort` injetado. Após a lógica de aprovação existente, chama `port.createActivationToken(command)`.

**Racional**: A criação do token de ativação é uma consequência da aprovação — faz sentido orquestrá-la no mesmo serviço de aplicação. Não duplica nenhum fluxo existente.

**Alternativas consideradas**:
- Listener de evento de domínio dentro do `mod-event`: **Rejeitado** — adiciona indireção sem benefício para um efeito colateral síncrono na mesma requisição.

---

## Decisão 5: Expiração e reemissão de token

**Decisão**: Validade padrão de 7 dias via propriedade `app.organizer-activation.expiry-days`. Não haverá endpoint de reemissão nesta entrega (P3 para versão futura). O administrador pode re-aprovar a solicitação para gerar novo token.

**Racional**: Simplifica a v1. A re-aprovação como mecanismo de reemissão já existe no fluxo atual.

---

## Decisão 6: Comportamento quando e-mail já tem conta — 409 no complete

**Decisão**: `POST /api/v1/auth/organizer-activation/complete` retorna `409 Conflict` se o e-mail já está cadastrado. O token NÃO é consumido (permanece válido). O usuário existente deve fazer login e o vínculo com o evento é feito pelo fluxo existente (`linkApprovedRequestToExistingUser` via `EventRequestAccountProvisioningPort`).

**Racional**: Preserva o fluxo já implementado para usuários existentes. O novo fluxo foca em novos usuários.
