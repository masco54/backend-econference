# Especificação de Funcionalidade: Cadastro de Organizador via Link de Ativação após Aprovação de Evento

**Feature Branch**: `002-organizer-activation-registration`  
**Created**: 2026-07-11  
**Status**: Draft  
**Input**: Descrição do usuário: "preciso que seja criado mais um tipo de cadastro, seria o cadastro de um organizador que vem pelo link de email, onde o organizador poderá concluir e ativar seu cadastro automaticamente. Apos o cadastro o perfil será automaticamente será ORGANIZADOR E PARTICIPANTE. Sendo que o default e ORGANIZADOR. Ao realizar o cadastro pelo link já vem preenchido o primeiro nome e sobrenome, email, telefone, sendo que somente telefone pode ser editavel. Deve ter uma mensagem informando que o cadastro é de organizador. O organizador não é convidado — ele solicitou a criação de um evento, o administrador aprovou a solicitação e, após a aprovação, o sistema envia o link para que o solicitante conclua seu cadastro e inicie seu evento."

## User Scenarios & Testing *(obrigatório)*

### User Story 1 - Validar Token de Ativação do Organizador Solicitante (Priority: P1)

Como frontend ou sistema consumidor, ao receber um token de ativação via query param (proveniente do link enviado após aprovação da solicitação de evento), preciso consultar o backend para validar o token e obter os dados pré-preenchidos do solicitante (firstName, lastName, email, phone — capturados no momento da solicitação), sabendo se o token é válido, expirado ou já utilizado.

**Why this priority**: A validação do token é pré-requisito de todo o fluxo. Sem ela, a página de cadastro não consegue carregar os dados do solicitante nem confirmar que o link de ativação é legítimo.

**Independent Test**: Exercitar `GET /api/v1/auth/organizer-activation/validate?token=<token>` com um token válido gerado após aprovação de solicitação de evento, e verificar que os dados do solicitante são retornados corretamente. Testar com token inválido, expirado e já utilizado e verificar resposta adequada sem vazar detalhes internos de implementação.

**Acceptance Scenarios**:

1. **Given** um token de ativação válido e não expirado gerado a partir de uma solicitação de evento aprovada, **When** o sistema recebe `GET /api/v1/auth/organizer-activation/validate?token=<token>`, **Then** retorna `200` com `{ valid: true, firstName, lastName, email, phone, eventName }`, onde `eventName` é o nome do evento solicitado.
2. **Given** um token expirado, **When** o sistema recebe a requisição de validação, **Then** retorna `200` com `{ valid: false, reason: "expired" }` sem expor detalhes internos.
3. **Given** um token já utilizado para concluir um cadastro anterior, **When** o sistema recebe a requisição de validação, **Then** retorna `200` com `{ valid: false, reason: "already-used" }`.
4. **Given** um token inexistente ou malformado, **When** o sistema recebe a requisição, **Then** retorna `200` com `{ valid: false, reason: "not-found" }` — sem distinguir entre token inexistente e token inválido para evitar enumeração.

---

### User Story 2 - Concluir Cadastro de Organizador via Token de Ativação (Priority: P1)

Como solicitante cujo evento foi aprovado pelo administrador, quero concluir meu cadastro na plataforma informando apenas a senha (e opcionalmente corrigindo o telefone), sendo automaticamente ativado como ORGANIZADOR e PARTICIPANTE e podendo iniciar a configuração do meu evento imediatamente.

**Why this priority**: Este é o fluxo central da funcionalidade. Sem ele, o link de ativação enviado após a aprovação não leva o solicitante a nenhum resultado concreto.

**Independent Test**: Exercitar `POST /api/v1/auth/organizer-activation/complete` com token válido gerado a partir de uma aprovação de solicitação de evento, senha e telefone opcional. Verificar que o usuário é criado com `emailVerified = true`, roles `[ORGANIZER, PARTICIPANT]`, role padrão `ORGANIZER`, e que o token é marcado como utilizado e não pode ser reutilizado.

**Acceptance Scenarios**:

1. **Given** um token de ativação válido (gerado após aprovação de solicitação de evento) para um e-mail que não possui conta na plataforma, **When** o solicitante envia `POST /api/v1/auth/organizer-activation/complete` com `{ token, password, phone? }`, **Then** o sistema cria a conta com `emailVerified = true`, roles `[ORGANIZER, PARTICIPANT]`, role padrão `ORGANIZER`, vincula o usuário ao evento aprovado, marca o token como utilizado, e retorna `201` com `{ userId, roles, eventId }`.
2. **Given** um token válido para um e-mail que já possui conta cadastrada, **When** o solicitante tenta concluir o cadastro, **Then** o sistema retorna `409` com mensagem informando que o e-mail já está cadastrado, orientando o usuário a fazer login para acessar o evento.
3. **Given** uma senha inválida (menos de 8 caracteres ou ausente), **When** o sistema recebe a requisição, **Then** retorna `422` com mensagem de validação clara e sem criar nenhum registro parcial.
4. **Given** um telefone fornecido pelo usuário durante o cadastro, **When** o cadastro é concluído com sucesso, **Then** o telefone do usuário é definido com o valor informado (sobrepondo o valor original da solicitação).
5. **Given** um cadastro concluído com sucesso, **When** o mesmo token é enviado novamente para `complete`, **Then** o sistema rejeita a requisição com `422` indicando que o token já foi utilizado.
6. **Given** um token expirado, **When** enviado para `complete`, **Then** o sistema retorna `422` informando que o token não é mais válido.

---

### User Story 3 - Aprovação de Solicitação de Evento Gera Token de Ativação (Priority: P2)

Como administrador da plataforma, ao aprovar uma solicitação de evento, o sistema deve gerar automaticamente um token de ativação vinculado à solicitação e disparar um e-mail para o solicitante com o link para concluir seu cadastro e iniciar a configuração do evento aprovado.

**Why this priority**: Sem a geração do token de ativação no momento da aprovação, o ciclo completo não existe — o solicitante não tem como saber que foi aprovado nem como acessar o evento.

**Independent Test**: Exercitar a aprovação de uma solicitação de evento via `POST /api/v1/events/requests/{id}/approve` (ou equivalente no `mod-event`) e verificar que o `mod-auth` gera e persiste o token de ativação, e que o e-mail de ativação é disparado para o e-mail do solicitante com o link correto.

**Acceptance Scenarios**:

1. **Given** uma solicitação de evento em estado apto para aprovação, **When** o administrador aprova a solicitação, **Then** o sistema (`mod-auth`, via port/contrato publicado pelo `mod-event`) gera um token de ativação UUID com validade de 7 dias (configurável), o vincula à solicitação e ao evento criado, e dispara o e-mail de ativação para o solicitante.
2. **Given** o e-mail de ativação disparado com sucesso, **When** o solicitante o recebe, **Then** o e-mail contém o nome do solicitante, o nome do evento aprovado, o link de ativação com o token e a data de expiração do link.
3. **Given** uma solicitação aprovada para um e-mail que já possui conta ativa na plataforma, **When** o sistema tenta gerar o token de ativação, **Then** o token é gerado normalmente (o fluxo de conclusão do cadastro lidará com a conta existente via `409`), e o e-mail de ativação é enviado normalmente.
4. **Given** falha no envio do e-mail de ativação, **When** o sistema processa a aprovação, **Then** o token é persistido e a aprovação é concluída — o e-mail é best-effort e sua falha não reverte a aprovação; a falha deve ser registrada em log.
5. **Given** um token de ativação expirado (7 dias sem uso), **When** o administrador precisa reenviar o acesso, **Then** deve existir mecanismo para reemitir um novo token (operação administrativa separada, fora do escopo desta story).

---

### Edge Cases

- Token expirado deve ser recusado tanto na validação (`validate`) quanto na conclusão (`complete`) do cadastro.
- Token já utilizado não pode ser reutilizado para criar nova conta, mesmo que a conta original seja deletada.
- O campo `email` não pode ser alterado pelo usuário durante o cadastro — o e-mail provém exclusivamente dos dados da solicitação de evento.
- Telefone é opcional na solicitação: se ausente no token de ativação, o campo permanece vazio e editável no frontend.
- Falha no envio do e-mail de ativação não deve reverter a aprovação da solicitação nem a geração do token; a operação é best-effort com log de falha.
- Tentativa de `complete` com token válido, mas e-mail já existente, deve retornar `409` sem criar conta duplicada, preservando a conta existente intacta.
- O sistema deve rejeitar tokens malformados com resposta genérica sem expor formato ou estrutura interna do token.
- Se a conclusão do cadastro falhar após criação do usuário (ex.: falha de persistência de roles ou de vínculo com o evento), a transação deve ser revertida — o usuário não deve existir em estado parcial.
- O e-mail de ativação MUST incluir o nome do solicitante, o nome do evento aprovado e a data de expiração do link.
- O token de ativação deve estar vinculado à solicitação de evento; não é possível gerar tokens de ativação de organizador avulsos sem uma solicitação aprovada associada.

## Requirements *(obrigatório)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir validar um token de ativação via `GET /api/v1/auth/organizer-activation/validate?token=<token>`, retornando os dados pré-preenchidos do solicitante (firstName, lastName, email, phone, eventName) quando válido.
- **FR-002**: O sistema MUST permitir concluir o cadastro do solicitante aprovado via `POST /api/v1/auth/organizer-activation/complete` com token, senha obrigatória e telefone opcional.
- **FR-003**: O sistema MUST criar o usuário com `emailVerified = true` e roles `[ORGANIZER, PARTICIPANT]` ao concluir o cadastro via token de ativação, sem exigir etapa adicional de verificação de e-mail.
- **FR-004**: O sistema MUST definir `ORGANIZER` como role principal/default do usuário criado via ativação de solicitação de evento aprovada.
- **FR-005**: O sistema MUST vincular o usuário recém-criado ao evento aprovado como organizador principal, na mesma transação da conclusão do cadastro.
- **FR-006**: O sistema MUST invalidar (marcar como utilizado) o token imediatamente após um cadastro concluído com sucesso, impedindo reutilização.
- **FR-007**: O sistema MUST impedir criação de conta via ativação quando o e-mail do token já está cadastrado, retornando `409`.
- **FR-008**: O sistema MUST validar a senha com as mesmas regras do cadastro regular (mínimo 8 caracteres).
- **FR-009**: O sistema MUST gerar automaticamente o token de ativação quando uma solicitação de evento é aprovada, via contrato publicado entre `mod-event` e `mod-auth` (port/adapter), sem acoplamento direto entre módulos.
- **FR-010**: O sistema MUST persistir o token de ativação com data de expiração configurável (default 7 dias via propriedade `app.organizer-activation.expiry-days`), vinculado à solicitação e ao evento aprovado.
- **FR-011** (renomeado): O sistema MUST disparar e-mail de ativação com link e nome do evento aprovado ao criar o token, usando o `EmailSenderPort` existente com novo template dedicado.
- **FR-012**: O sistema MUST preservar consistência de nomenclatura de endpoints, semântica HTTP e padrões de resposta de erro com os módulos existentes do backend.
- **FR-013**: O sistema MUST implementar a funcionalidade em `mod-auth` (validação/conclusão do cadastro) com o `mod-event` acionando a geração do token via contrato publicado em `shared`.
- **FR-014**: O sistema MUST preservar a direção de dependências da Clean Architecture: domínio independente de framework, adapters na borda, casos de uso orquestrados pela camada de aplicação via ports.
- **FR-015**: O sistema MUST usar o banco físico único existente, com a tabela `organizer_activation_tokens` no schema do `mod-auth`; não deve introduzir banco físico dedicado.

### Key Entities

- **OrganizerActivationToken**: Entidade nova no `mod-auth`. Campos: `id` (UUID), `token` (UUID único), `firstName`, `lastName`, `email`, `phone`, `eventId` (referência ao evento aprovado), `eventRequestId` (referência à solicitação), `expiresAt`, `usedAt` (nullable), `createdAt`. Pertence ao schema do `mod-auth`.
- **User**: Entidade existente. Ao concluir o cadastro via token de ativação: `emailVerified = true`, `roles = {ORGANIZER, PARTICIPANT}`.
- **EventRequest** (ownership: `mod-event`): Solicitação de evento. O fluxo de aprovação desta entidade dispara a criação do `OrganizerActivationToken` via contrato publicado.

## Success Criteria *(obrigatório)*

### Measurable Outcomes

- **SC-001**: Organizador convidado consegue concluir o cadastro em até 2 minutos após clicar no link do e-mail.
- **SC-002**: Endpoint de validação de token (`validate`) responde em p95 abaixo de 200 ms em condições locais de referência.
- **SC-003**: Endpoint de conclusão de cadastro (`complete`) responde em p95 abaixo de 500 ms em condições locais de referência.
- **SC-004**: Testes automatizados cobrem: validação de token (válido/expirado/usado/não-encontrado), conclusão do cadastro (sucesso/e-mail duplicado/token inválido/senha fraca), geração de convite (sucesso/e-mail duplicado/sem permissão/campos inválidos).
- **SC-005**: Token utilizado não pode ser reutilizado — verificado por teste de regressão dedicado.
- **SC-006**: O design final documenta a divisão domínio/aplicação, ports/adapters, direção de dependências e ownership do schema, sem violações de fronteira.

## Assumptions

- O módulo `mod-auth` é dono dos endpoints de validação e conclusão do cadastro, e da entidade `OrganizerActivationToken`. O módulo `mod-event` é responsável por acionar a geração do token ao aprovar uma solicitação, via contrato publicado em `shared`.
- Os dados pré-preenchidos (firstName, lastName, email, phone) provêm da solicitação de evento original, não de uma entrada manual do administrador.
- O e-mail de ativação utiliza o serviço `EmailSenderPort` existente, com um novo template HTML dedicado, incluindo o nome do evento aprovado.
- O banco físico único é mantido; a tabela `organizer_activation_tokens` será criada no schema do `mod-auth` via migration Flyway ou Liquibase, conforme padrão do projeto.
- A validade padrão do token é de 7 dias, configurável via propriedade `app.organizer-activation.expiry-days` no `application.yml`.
- Somente telefone pode ser editado pelo usuário durante o cadastro; `firstName`, `lastName` e `email` vêm dos dados da solicitação de evento e não podem ser alterados pelo usuário.
- H2 é suficiente para testes locais de integração, salvo quando houver comportamento específico de PostgreSQL que exija validação dedicada.
- A baseline local e o tráfego atual do módulo são representativos o bastante para validar os orçamentos de latência declarados.
- O `RegisterRequest` existente não será modificado; a nova funcionalidade usa endpoints e DTOs próprios para não impactar o fluxo de cadastro regular.
- O contrato entre `mod-event` e `mod-auth` para criação do token de ativação será definido em `shared` como um port/interface publicada, seguindo o padrão já existente (`EventRequestAccountProvisioningPort`).
