# Contrato de Autenticacao para Backend

Este documento resume o contrato que o frontend usa hoje para os fluxos de cadastro, login e recuperacao de senha.

## Base de API

- Base URL local: `http://localhost:3000/v1`
- Prefixo de autenticacao: `/auth`
- Arquivo fonte do contrato OpenAPI: `specs/master/contracts/auth.openapi.yaml`

## Regras de integracao

- O frontend envia requisicoes autenticadas com `withCredentials: true`.
- O backend deve permitir credenciais em CORS quando frontend e API estiverem em origens diferentes.
- A sessao deve ser mantida por cookie HttpOnly, preferencialmente `auth_token`.
- Em requisicoes mutaveis (`POST`, `PUT`, `PATCH`, `DELETE`), o frontend envia `X-XSRF-TOKEN` e `X-Requested-With: XMLHttpRequest`.
- Para login com email nao verificado, o backend deve responder `401` com `code: EMAIL_NOT_VERIFIED`.
- Em `forgot password`, o backend deve sempre responder `200` mesmo quando o email nao existir.

## 1. Cadastro

Endpoint: `POST /auth/register`

Request:

```json
{
  "firstName": "Joao",
  "lastName": "Silva",
  "email": "joao.silva@example.com",
  "phone": "+5511999999999",
  "password": "Senha1234",
  "invitationToken": "rev_inv_123456",
  "coOrganizerToken": "coorg_inv_123456"
}
```

Observacoes:

- `phone` e opcional.
- `invitationToken` e opcional e vincula convite de revisor.
- `coOrganizerToken` e opcional e vincula convite de coorganizador.
- O frontend valida `firstName` e `lastName` com 2-50 caracteres e `password` com minimo de 8 caracteres.

Sucesso: `201 Created`

```json
{
  "message": "Usuario registrado. Verifique seu email.",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "reviewerLinked": true,
  "coOrganizerLinked": false
}
```

Erros esperados:

- `400 Bad Request`: payload invalido
- `409 Conflict`: email ja cadastrado

Formato de erro:

```json
{
  "message": "Email ja esta em uso",
  "code": "EMAIL_ALREADY_EXISTS",
  "details": {}
}
```

## 2. Login

Endpoint: `POST /auth/login`

Request:

```json
{
  "email": "joao.silva@example.com",
  "password": "Senha1234"
}
```

Sucesso: `200 OK`

Headers esperados:

- `Set-Cookie: auth_token=...; HttpOnly; Secure; SameSite=Lax` ou politica equivalente

Body:

```json
{
  "message": "Login bem-sucedido",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "firstName": "Joao",
    "lastName": "Silva",
    "email": "joao.silva@example.com",
    "phone": "+5511999999999",
    "emailVerified": true,
    "roles": ["participant", "reviewer"],
    "createdAt": "2026-04-02T10:00:00Z",
    "updatedAt": "2026-04-02T10:00:00Z"
  }
}
```

Erros esperados:

- `401 Unauthorized`: credenciais invalidas
- `401 Unauthorized` com `code: EMAIL_NOT_VERIFIED`: usado pelo frontend para habilitar o CTA de reenviar verificacao

Exemplo de erro para email nao verificado:

```json
{
  "message": "Email nao verificado",
  "code": "EMAIL_NOT_VERIFIED"
}
```

## 3. Esqueceu a senha

Endpoint: `POST /auth/password/forgot`

Request:

```json
{
  "email": "joao.silva@example.com"
}
```

Sucesso: `200 OK`

```json
{
  "message": "Se o email existe, instrucoes foram enviadas"
}
```

Observacoes:

- Nao diferenciar email existente de inexistente no status HTTP nem na mensagem.
- O link enviado por email deve apontar para a rota do frontend `/reset-password?token=...`.

## 4. Redefinir senha

Embora voce tenha citado o inicio por `esqueceu a senha`, o frontend fecha o fluxo com redefinicao.

Endpoint: `POST /auth/password/reset`

Request:

```json
{
  "token": "reset_token_recebido_por_email",
  "newPassword": "NovaSenha1234"
}
```

Sucesso: `200 OK`

```json
{
  "message": "Senha redefinida com sucesso"
}
```

Erros esperados:

- `400 Bad Request`: token invalido ou expirado

## 5. Endpoints de apoio usados pelo frontend

### `POST /auth/verify-email`

Request:
```json
{ "token": "token_recebido_por_email" }
```
Sucesso: `200 OK` — `{ "message": "Email verified successfully" }`

### `POST /auth/resend-verification`

Request:
```json
{ "email": "joao.silva@example.com" }
```
Sucesso: `200 OK` — `{ "message": "Verification email sent" }`

### `GET /auth/me`

Requer cookie `auth_token`. Retorna dados completos do usuario autenticado.

Sucesso: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "Joao",
  "lastName": "Silva",
  "email": "joao.silva@example.com",
  "phone": "+5511999999999",
  "emailVerified": true,
  "roles": ["participant"]
}
```

### `POST /auth/logout`

Requer cookie `auth_token`. Invalida a sessao no servidor e limpa o cookie.

Sucesso: `204 No Content` com `Set-Cookie: auth_token=; Max-Age=0`

### `POST /auth/password/change`

Requer autenticacao. Altera a senha do usuario autenticado.

Request:
```json
{
  "currentPassword": "Senha1234",
  "newPassword": "NovaSenha5678"
}
```
Sucesso: `200 OK` — `{ "message": "Password changed successfully" }`

## 6. Endpoints de usuario

### `GET /users/{userId}`

Requer autenticacao. Retorna perfil publico (sem email, phone).

Sucesso: `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "firstName": "Joao",
  "lastName": "Silva",
  "roles": ["participant"]
}
```
Erros: `404 Not Found`

### `PATCH /users/{userId}`

Requer autenticacao. O usuario so pode atualizar o proprio perfil.

Request:
```json
{
  "firstName": "Joao",
  "lastName": "Santos",
  "phone": "+5511888888888"
}
```
Todos os campos sao opcionais. Campos nao enviados nao sao alterados.

Sucesso: `200 OK` — retorna `UserResponse` completo
Erros: `400`, `401`, `403` (nao e o dono do perfil)

### `GET /users/me/roles`

Requer autenticacao. Retorna os papeis do usuario autenticado.

Sucesso: `200 OK`
```json
{
  "roles": ["participant", "reviewer"]
}
```

## Requisitos de cookie

- Nome: `auth_token`
- `HttpOnly: true`
- `Secure: true` (exceto em ambiente local)
- `SameSite: Lax`
- `Max-Age`: conforme `app.jwt.expiration-days` (padrao: 7 dias)
- `Path: /`