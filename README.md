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

Ao subir localmente, a aplicação cria um usuário administrador bootstrap se ele ainda não existir:

- email: `admin@econference.local`
- senha: `Admin@123`

Você pode sobrescrever ou desabilitar esse bootstrap via variáveis de ambiente:

- `BOOTSTRAP_ADMIN_ENABLED=false`
- `BOOTSTRAP_ADMIN_EMAIL=...`
- `BOOTSTRAP_ADMIN_PASSWORD=...`
- `BOOTSTRAP_ADMIN_FIRST_NAME=...`
- `BOOTSTRAP_ADMIN_LAST_NAME=...`
- `BOOTSTRAP_ADMIN_PHONE=...`

Se já existir um usuário com o mesmo email, o bootstrap apenas garante os papéis `ADMIN` e `PARTICIPANT` e marca o email como verificado; a senha configurada vale para a primeira criação.

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

- `GET /api/v1/auth/health`
- `GET /api/v1/events/health`
- `GET /api/v1/submissions/health`
- `GET /api/v1/reviews/health`
- `GET /api/v1/committees/health`
- `GET /api/v1/registrations/health`

## Módulo Event — Solicitação de Eventos

### Endpoints públicos

| Método | Rota | Descrição | Auth |
|--------|------|-----------|------|
| POST | `/api/v1/public/event-requests` | Registrar uma nova solicitação pública de evento | Não |
| POST | `/api/v1/public/event-requests/track` | Consultar a solicitação por código e e-mail | Não |
| GET | `/api/v1/public/event-requests/activate?token=...` | Validar token de ativação | Não |
| POST | `/api/v1/public/event-requests/activate` | Concluir a ativação e vincular o organizador ao evento | Não |

### Endpoints administrativos

| Método | Rota | Descrição | Auth |
|--------|------|-----------|------|
| GET | `/api/v1/admin/event-requests` | Listar solicitações com paginação, filtro por status e busca textual | ADMIN |
| GET | `/api/v1/admin/event-requests/{requestId}` | Obter o detalhe de uma solicitação | ADMIN |
| POST | `/api/v1/admin/event-requests/{requestId}/approve` | Aprovar a solicitação, criar o evento pendente e emitir token | ADMIN |
| POST | `/api/v1/admin/event-requests/{requestId}/reject` | Rejeitar a solicitação com motivo obrigatório | ADMIN |
| POST | `/api/v1/admin/event-requests/{requestId}/add-note` | Adicionar nota administrativa e mover para análise quando aplicável | ADMIN |
| POST | `/api/v1/admin/event-requests/{requestId}/reissue-token` | Reemitir token de ativação expirado ou inválido | ADMIN |

### Regras principais

- Solicitações abertas duplicadas para a mesma combinação `requesterEmail + eventName` são rejeitadas com `409 Conflict`.
- Validações de payload e transições inválidas retornam `400 Bad Request`.
- Acompanhamento público usa a combinação de código e e-mail e retorna `404 Not Found` para combinações inexistentes.
- A aprovação cria um `Event` em estado `PENDING` e associa `linkedEventId` à solicitação.
- Endpoints administrativos exigem autenticação e papel `ADMIN`; acessos sem privilégio retornam `403 Forbidden`.

## Módulo Auth — Fluxo de Autenticação

### Endpoints

| Método | Rota | Descrição | Auth |
|--------|------|-----------|------|
| POST | `/api/v1/auth/register` | Cadastro de usuário | Não |
| POST | `/api/v1/auth/verify-email` | Verificação de email | Não |
| POST | `/api/v1/auth/resend-verification` | Reenviar email de verificação | Não |
| POST | `/api/v1/auth/login` | Login (retorna cookie `auth_token`) | Não |
| POST | `/api/v1/auth/logout` | Logout (invalida sessão + limpa cookie) | Sim |
| GET | `/api/v1/auth/me` | Dados do usuário autenticado | Sim |
| POST | `/api/v1/auth/password/forgot` | Solicitar reset de senha | Não |
| POST | `/api/v1/auth/password/reset` | Redefinir senha com token | Não |
| POST | `/api/v1/auth/password/change` | Alterar senha (autenticado) | Sim |
| GET | `/api/v1/users/{userId}` | Perfil público do usuário | Sim |
| PATCH | `/api/v1/users/{userId}` | Atualizar próprio perfil | Sim |
| GET | `/api/v1/users/me/roles` | Papéis do usuário autenticado | Sim |

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
