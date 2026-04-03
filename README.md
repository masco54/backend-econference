# eConference Backend

Monólito modular com Spring Boot 3.4 + Java 24.

## Módulos

| Módulo | Descrição |
|--------|-----------|
| `shared` | Kernel compartilhado: entidade base, exceções, config |
| `mod-auth` | Autenticação, JWT, usuários |
| `mod-event` | Criação e gestão de eventos |
| `mod-submission` | Submissões de artigos |
| `mod-review` | Avaliação e revisão |
| `mod-committee` | Comitê organizador |
| `mod-registration` | Inscrições em eventos |
| `application` | Ponto de entrada – monta todos os módulos |

## Rodar local (H2 em memória)

```bash
./mvnw spring-boot:run -pl application
```

## Rodar local com PostgreSQL

```bash
docker compose up -d
./mvnw spring-boot:run -pl application -Dspring-boot.run.profiles=postgres
```

## Build

```bash
./mvnw clean package
```

## Endpoints de health por módulo

- `GET /api/auth/health`
- `GET /api/events/health`
- `GET /api/submissions/health`
- `GET /api/reviews/health`
- `GET /api/committees/health`
- `GET /api/registrations/health`

## Módulo Auth — Fluxo de Autenticação

### Endpoints

| Método | Rota | Descrição | Auth |
|--------|------|-----------|------|
| POST | `/auth/register` | Cadastro de usuário | Não |
| POST | `/auth/verify-email` | Verificação de email | Não |
| POST | `/auth/resend-verification` | Reenviar email de verificação | Não |
| POST | `/auth/login` | Login (retorna cookie `auth_token`) | Não |
| POST | `/auth/logout` | Logout (invalida sessão + limpa cookie) | Sim |
| GET | `/auth/me` | Dados do usuário autenticado | Sim |
| POST | `/auth/password/forgot` | Solicitar reset de senha | Não |
| POST | `/auth/password/reset` | Redefinir senha com token | Não |
| POST | `/auth/password/change` | Alterar senha (autenticado) | Sim |
| GET | `/users/{userId}` | Perfil público do usuário | Sim |
| PATCH | `/users/{userId}` | Atualizar próprio perfil | Sim |
| GET | `/users/me/roles` | Papéis do usuário autenticado | Sim |

### Autenticação via Cookie

- O login retorna um `Set-Cookie: auth_token=<jwt>; HttpOnly; Secure; SameSite=Lax`.
- Requisições autenticadas devem enviar `withCredentials: true`.
- O logout limpa o cookie e invalida a sessão no servidor.

### Papéis Disponíveis

`PARTICIPANT`, `ORGANIZER`, `REVIEWER`, `ADMIN`

### Configuração SMTP

Variáveis de ambiente necessárias para envio de email:

| Variável | Descrição |
|----------|-----------|
| `MAIL_HOST` | Servidor SMTP |
| `MAIL_PORT` | Porta SMTP (ex: 465) |
| `MAIL_USERNAME` | Usuário SMTP |
| `MAIL_PASSWORD` | Senha SMTP |
