# Quickstart — Verificação Manual do Módulo Auth

Passos para validar os fluxos de autenticação localmente.

## Pré-requisitos

```bash
./mvnw spring-boot:run -pl application
```

A aplicação roda em `http://localhost:8080` com H2 em memória.

---

## 1. Cadastro

```bash
curl -s -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "João",
    "lastName": "Silva",
    "email": "joao@example.com",
    "password": "Senha1234"
  }'
```

Resposta esperada: `201 Created` com `userId`, `reviewerLinked: false`, `coOrganizerLinked: false`.

---

## 2. Verificação de Email

Consulte o log da aplicação para encontrar o token de verificação (em modo dev, o email é logado).

```bash
curl -s -X POST http://localhost:8080/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{ "token": "<TOKEN_DO_LOG>" }'
```

Resposta esperada: `200 OK` com `"Email verified successfully"`.

---

## 3. Login

```bash
curl -s -c cookies.txt -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "joao@example.com",
    "password": "Senha1234"
  }'
```

Resposta esperada: `200 OK` com dados do usuário. O arquivo `cookies.txt` armazena o cookie `auth_token`.

---

## 4. Usuário Autenticado

```bash
curl -s -b cookies.txt http://localhost:8080/auth/me
```

Resposta esperada: `200 OK` com dados completos do usuário autenticado.

---

## 5. Papéis do Usuário

```bash
curl -s -b cookies.txt http://localhost:8080/users/me/roles
```

Resposta esperada: `200 OK` com `{ "roles": ["participant"] }`.

---

## 6. Atualizar Perfil

```bash
curl -s -b cookies.txt -X PATCH http://localhost:8080/users/<USER_ID> \
  -H "Content-Type: application/json" \
  -d '{ "firstName": "João Pedro" }'
```

Resposta esperada: `200 OK` com dados atualizados.

---

## 7. Perfil Público

```bash
curl -s -b cookies.txt http://localhost:8080/users/<USER_ID>
```

Resposta esperada: `200 OK` com `id`, `firstName`, `lastName`, `roles` (sem email/phone).

---

## 8. Alterar Senha

```bash
curl -s -b cookies.txt -X POST http://localhost:8080/auth/password/change \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "Senha1234",
    "newPassword": "NovaSenha5678"
  }'
```

Resposta esperada: `200 OK` com `"Password changed successfully"`.

---

## 9. Esqueceu a Senha

```bash
curl -s -X POST http://localhost:8080/auth/password/forgot \
  -H "Content-Type: application/json" \
  -d '{ "email": "joao@example.com" }'
```

Resposta esperada: `200 OK` com `"If the email exists, a reset link has been sent"`.

Consulte o log para obter o token de reset.

---

## 10. Redefinir Senha

```bash
curl -s -X POST http://localhost:8080/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token": "<RESET_TOKEN_DO_LOG>",
    "newPassword": "SenhaReset123"
  }'
```

Resposta esperada: `200 OK` com `"Password reset successfully"`.

---

## 11. Logout

```bash
curl -s -b cookies.txt -X POST http://localhost:8080/auth/logout
```

Resposta esperada: `204 No Content`. Cookie `auth_token` limpo.

---

## 12. Verificar Rejeição Pós-Logout

```bash
curl -s -b cookies.txt http://localhost:8080/auth/me
```

Resposta esperada: `401 Unauthorized` (sessão invalidada).
