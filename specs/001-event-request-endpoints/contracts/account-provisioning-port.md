# Contract: Account Provisioning Port

## Purpose

Definir a colaboração explícita entre `mod-event` e `mod-auth` durante a ativação de uma solicitação aprovada, sem acesso direto às tabelas de autenticação.

## Owner and Consumer

- **Owner**: `mod-auth`
- **Consumer**: `mod-event`

## Required Operations

| Operation | Input | Output | Notes |
|-----------|-------|--------|-------|
| `findUserByEmail` | e-mail do solicitante | existência da conta, `userId`, papéis e status elegível | usado para decidir entre vínculo e provisionamento |
| `provisionOrganizerAccount` | dados do solicitante aprovado + senha inicial | `userId`, indicador `userCreated`, papéis resultantes | cria conta quando ainda não existe usuário |
| `linkApprovedRequestToExistingUser` | `userId`, `eventId`, dados da solicitação aprovada | confirmação de vínculo e papéis | usado quando a conta já existe |

## Behavioral Rules

- `mod-event` não pode ler nem escrever diretamente nas tabelas `users`, `user_roles`, `user_sessions` ou tokens de `mod-auth`.
- `mod-auth` continua responsável por validação de senha, criação de conta, atribuição do papel de organizador e regras de segurança relacionadas ao usuário.
- `mod-event` continua responsável por validar a solicitação, garantir o estado `APPROVED`, controlar o token de ativação e vincular o `linkedUserId` resultante.
- Falhas do contrato devem ser propagadas como erro de aplicação explícito, sem deixar a solicitação em estado parcialmente ativado.

## Failure Modes

- Usuário existente sem elegibilidade para vínculo -> erro de negócio explícito.
- Senha inválida para provisionamento -> erro de validação explícito.
- Falha interna em `mod-auth` durante provisionamento/vínculo -> operação deve ser transacionalmente abortada em `mod-event`.
