# Especificação da Funcionalidade: Módulo de Autenticação (mod-auth)

**ID da Funcionalidade**: auth-module-complete  
**Criado em**: 2026-04-02  
**Status**: Rascunho  
**Módulo**: `econference-mod-auth`  

---

## Visão Geral

Completar a implementação de backend do Módulo de Autenticação (`mod-auth`) da plataforma E-Conference. Este módulo é responsável por cadastro de usuários, verificação de identidade, gerenciamento de sessão, ciclo de vida de senha e gerenciamento de perfil do usuário. Ele funciona como a base de segurança da qual todos os outros módulos dependem.

Atualmente o módulo possui apenas uma estrutura inicial (entidade, repositório stub e endpoint básico de health), mas ainda não possui a lógica de negócio, os filtros de segurança, a integração de email nem toda a superfície REST definida no contrato da API.

---

## Problema

O frontend da plataforma E-Conference já está construído e já envia requisições contra o contrato da API de autenticação. Sem um módulo de autenticação de backend funcional, os usuários não conseguem se cadastrar, fazer login, verificar suas identidades nem ser reconhecidos pelos demais módulos. Todos os outros módulos da plataforma (submissões, revisões, eventos) ficam bloqueados até que a autenticação esteja pronta.

---

## Objetivos

- Entregar um módulo de autenticação totalmente funcional que satisfaça o contrato de API definido em `auth.openapi.yaml`
- Permitir que o frontend execute todos os fluxos de autenticação sem modificações
- Proteger todos os endpoints não públicos com autenticação de sessão baseada em cookie
- Suportar usuários com múltiplos papéis (participant, organizer, reviewer, admin), conforme exigido pelos outros módulos
- Fornecer a base para atribuição de papéis via convite (reviewer, co-organizer) usada por outros módulos

---

## Fora de Escopo

- Implementação de OAuth2 / login social (fase futura)
- Painéis administrativos de gestão de usuários ou operações em massa
- Fluxos de promoção de papéis (tratados por outros módulos; este módulo apenas armazena e retorna papéis)
- Autenticação em dois fatores (fase futura)
- Infraestrutura de auditoria de logs (fase futura)

---

## Atores

| Ator | Descrição |
|-------|-------------|
| **Visitor** | Usuário não autenticado navegando na plataforma |
| **Registered User** | Usuário com conta confirmada (email verificado) |
| **Unverified User** | Usuário que se cadastrou, mas ainda não verificou o email |
| **Authenticated Session** | Sessão ativa de login identificada pelo cookie `auth_token` |
| **Other Platform Modules** | Módulos internos (mod-submission, mod-review etc.) que consomem a identidade do usuário fornecida por este módulo |

---

## Cenários de Usuário e Testes

### Cenário 1: Cadastro de Novo Usuário

**Dado que** um visitante informa nome, sobrenome, email e senha válidos  
**Quando** envia o formulário de cadastro  
**Então** uma nova conta é criada, um email de verificação é enviado e o sistema retorna resposta 201 com o novo ID do usuário  

**Critérios de aceitação**:
- Email duplicado retorna 409 com código `EMAIL_ALREADY_EXISTS`
- Payload inválido (campo obrigatório ausente, senha < 8 caracteres, nome/sobrenome < 2 ou > 50 caracteres) retorna 400
- Telefone é opcional; se informado, deve seguir formato internacional
- Se um `invitationToken` válido for informado, o usuário é vinculado ao convite de revisor e `reviewerLinked: true` é retornado
- Se um `coOrganizerToken` válido for informado, o usuário é vinculado ao convite de coorganizador e `coOrganizerLinked: true` é retornado
- Tokens de convite inválidos ou expirados são ignorados silenciosamente (o cadastro é concluído e o vínculo é apenas ignorado)

---

### Cenário 2: Verificação de Email

**Dado que** um usuário recebeu um email de verificação após o cadastro  
**Quando** clica no link e envia o token de verificação  
**Então** sua conta é marcada como verificada e ele pode prosseguir para o login  

**Critérios de aceitação**:
- Token válido marca `emailVerified = true` e retorna 200
- Token expirado retorna 400
- Token inválido ou já utilizado retorna 400
- Tokens de verificação expiram após um período definido (padrão: 24 horas)

---

### Cenário 3: Reenvio de Email de Verificação

**Dado que** um usuário não verificado não recebeu ou perdeu o email de verificação  
**Quando** solicita um novo email de verificação usando seu email cadastrado  
**Então** um novo token de verificação é gerado e enviado  

**Critérios de aceitação**:
- Se o email for encontrado e a conta ainda não estiver verificada, um novo email é enviado e 200 é retornado
- Se o email não for encontrado, 404 é retornado
- Tokens de verificação anteriores são invalidados no reenvio

---

### Cenário 4: Login do Usuário

**Dado que** um usuário cadastrado e com email verificado informa credenciais corretas  
**Quando** envia email e senha  
**Então** uma sessão é criada via cookie HttpOnly e o objeto completo do usuário é retornado  

**Critérios de aceitação**:
- Credenciais corretas → 200 com `Set-Cookie: auth_token=...; HttpOnly; Secure; SameSite=Lax` e objeto completo do usuário
- Senha incorreta → 401 com erro genérico (sem indicar se o email existe)
- Email não verificado → 401 com `code: EMAIL_NOT_VERIFIED` (o frontend usa isso para mostrar a CTA de reenvio da verificação)
- O cookie de sessão deve ser HttpOnly para impedir acesso via JavaScript
- O cookie de sessão deve ser Secure (somente HTTPS) em ambientes não locais

---

### Cenário 5: Logout

**Dado que** um usuário autenticado possui uma sessão ativa  
**Quando** faz logout  
**Então** a sessão é invalidada e o cookie é limpo  

**Critérios de aceitação**:
- 204 é retornado em logout bem-sucedido
- O cookie `auth_token` é limpo na resposta
- Requisições subsequentes com o mesmo token são rejeitadas com 401
- Chamar logout sem uma sessão válida retorna 401

---

### Cenário 6: Obter Usuário Atual

**Dado que** um usuário está autenticado  
**Quando** solicita o próprio perfil  
**Então** o objeto completo do usuário (id, nome, email, telefone, emailVerified, roles e timestamps) é retornado  

**Critérios de aceitação**:
- Retorna 200 com o schema User
- Retorna 401 se não existir sessão válida

---

### Cenário 7: Esqueci a Senha

**Dado que** um usuário esqueceu sua senha  
**Quando** informa seu endereço de email para recuperação  
**Então** se o email existir, um email de recuperação é enviado; independentemente disso, a resposta é sempre 200  

**Critérios de aceitação**:
- Sempre retorna 200 com a mesma mensagem, independentemente de o email existir ou não (evita enumeração de contas)
- Se o email existir, um email de redefinição de senha é enviado com link para a página de reset do frontend
- O link de recuperação inclui o token de reset como parâmetro de query
- Tokens de reset expiram após um período definido (padrão: 1 hora)

---

### Cenário 8: Redefinir Senha

**Dado que** um usuário recebeu um email de redefinição de senha  
**Quando** envia o token de reset e sua nova senha  
**Então** sua senha é atualizada e o token é consumido  

**Critérios de aceitação**:
- Token válido + nova senha → 200 e senha atualizada
- Token expirado ou inválido → 400
- Comprimento mínimo de 8 caracteres para a nova senha é obrigatório
- Após reset bem-sucedido, todas as sessões existentes do usuário são invalidadas

---

### Cenário 9: Alterar Senha (Autenticado)

**Dado que** um usuário autenticado conhece sua senha atual  
**Quando** envia a senha atual e a nova senha  
**Então** sua senha é atualizada  

**Critérios de aceitação**:
- Senha atual correta + nova senha válida → 200
- Senha atual incorreta → 400
- Requisição não autenticada → 401
- Comprimento mínimo de 8 caracteres é obrigatório para a nova senha

---

### Cenário 10: Obter Usuário por ID (Informações Públicas)

**Dado que** um usuário autenticado quer visualizar o perfil público de outro usuário  
**Quando** solicita um usuário por UUID  
**Então** apenas os campos públicos (id, firstName, lastName, roles) são retornados  

**Critérios de aceitação**:
- Retorna 200 com o schema `PublicUser` (sem email e sem telefone)
- Retorna 404 se o usuário não existir
- Exige autenticação (401 se não houver sessão válida)

---

### Cenário 11: Atualizar o Próprio Perfil

**Dado que** um usuário está autenticado  
**Quando** envia campos atualizados do perfil (firstName, lastName, phone)  
**Então** seu perfil é atualizado e o objeto completo atualizado do usuário é retornado  

**Critérios de aceitação**:
- Usuários só podem atualizar o próprio perfil; tentar atualizar o perfil de outro usuário retorna 403
- Retorna 200 com o schema User atualizado em caso de sucesso
- Payload inválido (nome < 2 ou > 50 caracteres, telefone inválido) retorna 400
- O email não pode ser alterado por este endpoint
- Requisição não autenticada retorna 401

---

### Cenário 12: Listar os Próprios Papéis

**Dado que** um usuário está autenticado  
**Quando** solicita seus papéis ativos  
**Então** a lista de papéis atuais é retornada  

**Critérios de aceitação**:
- Retorna 200 com o array de papéis
- Retorna 401 se não estiver autenticado
- A lista de papéis reflete todos os papéis atualmente atribuídos (pode incluir múltiplos)

---

### Cenário 13: Proteção contra CSRF

**Dado que** o frontend envia requisições mutáveis (POST, PUT, PATCH, DELETE) com o header `X-XSRF-TOKEN`  
**Quando** o backend processa essas requisições  
**Então** o token CSRF é validado  

**Critérios de aceitação**:
- Requisições mutáveis sem um token CSRF válido são rejeitadas
- CORS é configurado para permitir credenciais a partir da(s) origem(ns) registrada(s) do frontend
- O header `X-Requested-With: XMLHttpRequest` é aceito

---

## Requisitos Funcionais

### FR-01: Cadastro de Usuário

- O sistema deve aceitar requisições de cadastro com firstName, lastName, email, password (obrigatórios) e phone, invitationToken, coOrganizerToken (opcionais)
- firstName e lastName devem ter entre 2 e 50 caracteres
- password deve ter no mínimo 8 e no máximo 100 caracteres
- phone, quando informado, deve seguir o formato internacional
- O email deve ser único entre todos os usuários; email duplicado deve retornar 409 com `code: EMAIL_ALREADY_EXISTS`
- Após o cadastro com sucesso, um email de verificação deve ser enviado ao endereço do usuário
- A resposta deve incluir o UUID do novo usuário e indicadores booleanos para `reviewerLinked` e `coOrganizerLinked`

### FR-02: Verificação de Email

- O sistema deve gerar um token de verificação único e temporário no momento do cadastro
- Tokens de verificação devem expirar após 24 horas
- Enviar um token válido, não usado e não expirado deve marcar o email do usuário como verificado
- Enviar um token expirado ou inválido deve retornar erro 400
- Em caso de reenvio, o token emitido anteriormente deve ser invalidado e um novo token deve ser gerado

### FR-03: Gerenciamento de Sessão (Login / Logout)

- Login bem-sucedido deve emitir um cookie HttpOnly chamado `auth_token`
- O cookie deve possuir a flag Secure em ambientes não locais
- A política SameSite do cookie deve ser `Lax` ou `Strict`
- Login com email não verificado deve retornar 401 com `code: EMAIL_NOT_VERIFIED`
- Login com credenciais inválidas deve retornar 401 com erro genérico, sem diferenciar email inexistente de senha incorreta
- Logout deve invalidar a sessão para que o cookie não possa mais ser usado
- No logout, a resposta deve limpar o cookie `auth_token`

### FR-04: Gerenciamento de Senha

- Requisições de forgot-password devem sempre retornar 200, independentemente de o email existir
- Quando o email existir, um token de reset deve ser gerado e um email de recuperação deve ser enviado
- O link de recuperação enviado por email deve apontar para a rota de reset-password do frontend com o token como parâmetro de query
- Tokens de reset devem expirar após 1 hora
- Após um reset de senha bem-sucedido, todas as sessões existentes do usuário devem ser invalidadas
- Alteração de senha autenticada deve verificar a senha atual antes de aceitar a nova

### FR-05: Perfil do Usuário

- O endpoint do usuário autenticado (`GET /auth/me`) deve retornar o schema completo de User
- O endpoint público de usuário (`GET /users/{userId}`) deve retornar apenas os campos públicos (id, firstName, lastName, roles)
- Um usuário só pode atualizar o próprio perfil; atualizações entre usuários devem retornar 403
- O email não pode ser alterado por meio do endpoint de atualização de perfil
- O endpoint de listagem de papéis deve retornar todos os papéis atualmente ativos do usuário autenticado

### FR-06: Suporte a Múltiplos Papéis

- Um usuário pode possuir múltiplos papéis simultaneamente (por exemplo, participant + reviewer)
- Os papéis são aditivos; atribuir um novo papel não remove os já existentes
- Papéis disponíveis: `participant`, `organizer`, `reviewer`, `admin`
- Novos usuários cadastrados sem tokens de convite recebem o papel `participant` por padrão
- A atribuição de papéis via tokens de convite é tratada no momento do cadastro e delegada ao sistema de convites correspondente

### FR-07: Segurança

- Senhas devem ser armazenadas como hashes com salt (bcrypt)
- O token JWT (ou identificador de sessão) embutido no cookie `auth_token` deve ser validado em toda requisição protegida
- A proteção contra CSRF deve ser aplicada a todos os endpoints mutáveis
- CORS deve ser configurado para permitir credenciais (`withCredentials: true`) a partir da origem do frontend
- Requisições não autenticadas a endpoints protegidos devem retornar 401

### FR-08: Envio de Email

- O sistema deve enviar os seguintes emails transacionais:
  1. **Verificação de email** – no cadastro e no reenvio de verificação
  2. **Recuperação de senha** – no forgot-password, quando o email existir
- Os emails devem conter um link claro de chamada para ação ou um token
- Falhas no envio de email não devem quebrar a resposta HTTP (best-effort, com log)

---

## Modelo de Dados

### Entidade: User

| Campo | Tipo | Restrições | Observações |
|-------|------|-------------|-------|
| id | UUID | Primary key, auto-generated | |
| firstName | String | Not null, 2–50 chars | |
| lastName | String | Not null, 2–50 chars | |
| email | String | Not null, unique | Immutable after registration |
| phone | String | Nullable | International format |
| passwordHash | String | Not null | Bcrypt hash, never exposed |
| emailVerified | Boolean | Not null, default false | |
| roles | Set\<Role\> | Not null, min 1 | Enum: participant, organizer, reviewer, admin |
| createdAt | Timestamp | Auto-set on create | |
| updatedAt | Timestamp | Auto-set on update | |

> **Observação**: A entidade `User` atual usa um `name` simplificado e um único enum `UserRole` (`ADMIN`, `USER`). A entidade precisa ser refatorada para corresponder ao schema acima. O enum `UserRole` deve ser substituído pelo modelo de múltiplos papéis compatível com o contrato (`participant`, `organizer`, `reviewer`, `admin`).

### Entidade: EmailVerificationToken

| Campo | Tipo | Restrições | Observações |
|-------|------|-------------|-------|
| id | UUID | Primary key | |
| userId | UUID | Foreign key → User | |
| token | String | Unique, not null | Randomly generated |
| expiresAt | Timestamp | Not null | 24 hours from creation |
| usedAt | Timestamp | Nullable | Set when consumed |
| createdAt | Timestamp | Auto-set | |

### Entidade: PasswordResetToken

| Campo | Tipo | Restrições | Observações |
|-------|------|-------------|-------|
| id | UUID | Primary key | |
| userId | UUID | Foreign key → User | |
| token | String | Unique, not null | Randomly generated |
| expiresAt | Timestamp | Not null | 1 hour from creation |
| usedAt | Timestamp | Nullable | Set when consumed |
| createdAt | Timestamp | Auto-set | |

### Entidade: UserSession (ou baseada em JWT, ver Premissas)

| Campo | Tipo | Restrições | Observações |
|-------|------|-------------|-------|
| id | UUID | Primary key | |
| userId | UUID | Foreign key → User | |
| tokenHash | String | Unique, not null | Hash of the `auth_token` value |
| expiresAt | Timestamp | Not null | |
| invalidatedAt | Timestamp | Nullable | Set on logout or password reset |
| createdAt | Timestamp | Auto-set | |

---

## Superfície da API

Todos os endpoints seguem o contrato definido em `auth.openapi.yaml`. A tabela abaixo resume toda a superfície que este módulo deve expor:

| Método | Path | Autenticação Necessária | Descrição |
|--------|------|-------------------------|-----------|
| POST | `/auth/register` | Não | Cadastrar novo usuário |
| POST | `/auth/verify-email` | Não | Verificar token de email |
| POST | `/auth/resend-verification` | Não | Reenviar email de verificação |
| POST | `/auth/login` | Não | Autenticar e emitir cookie de sessão |
| POST | `/auth/logout` | Sim | Invalidar sessão |
| GET | `/auth/me` | Sim | Obter usuário atual |
| POST | `/auth/password/forgot` | Não | Solicitar reset de senha |
| POST | `/auth/password/reset` | Não | Redefinir senha via token |
| POST | `/auth/password/change` | Sim | Alterar senha (autenticado) |
| GET | `/users/{userId}` | Sim | Obter informações públicas do usuário por ID |
| PATCH | `/users/{userId}` | Sim | Atualizar o próprio perfil |
| GET | `/users/me/roles` | Sim | Listar os próprios papéis ativos |

---

## Contrato de Resposta de Erro

Todas as respostas de erro devem seguir o schema padrão abaixo:

```json
{
  "message": "Mensagem legível para humanos",
  "code": "MACHINE_READABLE_CODE",
  "details": {}
}
```

Códigos de erro principais:
- `EMAIL_ALREADY_EXISTS` – Cadastro com email duplicado
- `EMAIL_NOT_VERIFIED` – Tentativa de login antes da verificação do email
- `UNAUTHORIZED` – Sessão ausente ou inválida
- `NOT_FOUND` – Recurso inexistente
- `INVALID_TOKEN` – Token de verificação/reset inválido ou expirado

---

## Critérios de Sucesso

1. Todos os 12 endpoints da API respondem com os status HTTP e corpos de resposta corretos, conforme definido em `auth.openapi.yaml`
2. Um novo usuário consegue completar todo o fluxo de cadastro até login (register → verify email → login) sem intervenção manual
3. Um usuário que esqueceu a senha consegue recuperar acesso em uma única jornada (request reset → receive email → reset → login)
4. As sessões de login ficam protegidas contra cross-site scripting: o cookie `auth_token` não é acessível via JavaScript
5. Qualquer tentativa de acessar endpoint protegido sem sessão válida é sempre rejeitada
6. Usuários só conseguem modificar o próprio perfil; tentativas de alterar o perfil de outro usuário são sempre rejeitadas
7. O endpoint de forgot-password nunca revela se um endereço de email está cadastrado
8. O sistema retorna corretamente papéis diferentes para usuários vinculados a convites de reviewer ou co-organizer no momento do cadastro
9. O envio de email não causa falha na requisição: a resposta HTTP é concluída independentemente da entrega do email
10. Todo armazenamento de senha usa hash irreversível; nenhuma senha em texto puro é persistida ou registrada em log

---

## Dependências e Integrações

| Dependência | Direção | Observações |
|-------------|---------|-------------|
| `econference-shared` | Interna | `BaseEntity`, exceções compartilhadas (ex.: `ResourceNotFoundException`) |
| `mod-registration` | Saída (futuro) | A validação de token de coorganizador pode delegar para este módulo |
| `mod-review` | Saída (futuro) | A validação de token de reviewer pode delegar para este módulo |
| Banco de dados (PostgreSQL) | Infraestrutura | Dados de usuário, tokens e sessões persistidos via JPA |
| Serviço de email | Infraestrutura | Entrega de email transacional para verificação e reset de senha |
| Frontend | Consumidor | Deve respeitar o contrato da API; usa `withCredentials: true` |

---

## Premissas

1. **Mecanismo de sessão**: Tokens JWT armazenados em cookies HttpOnly. O JWT é validado de forma stateless por requisição, mas a invalidação de sessão (logout, reset de senha) exige uma denylist server-side ou um registro de sessão no banco. Esta spec assume o uso de uma tabela leve de sessões para suportar invalidação sem depender da expiração do token.
2. **Expiração padrão dos tokens**: Tokens de verificação de email expiram em 24 horas; tokens de reset de senha expiram em 1 hora. Esses valores devem ser configuráveis via propriedades da aplicação.
3. **Papel padrão**: Um usuário cadastrado sem token de convite recebe o papel `participant`.
4. **Validação de token de convite**: Se um `invitationToken` ou `coOrganizerToken` for informado, mas estiver inválido ou expirado, o cadastro continua com o token ignorado silenciosamente. Não deve haver retorno 400 por token de convite inválido.
5. **Estratégia de CSRF**: O frontend envia o header `X-XSRF-TOKEN` em requisições mutáveis. O backend deve emitir e validar um token CSRF, por exemplo com `CookieCsrfTokenRepository` do Spring Security usando `withHttpOnlyFalse()` para permitir leitura pelo frontend.
6. **Origens de CORS**: As origens do frontend devem ser configuradas via propriedades da aplicação, e não hardcoded. Em desenvolvimento local usa-se `http://localhost:3000`.
7. **Templates de email**: Templates HTML simples são suficientes para a implementação inicial. O conteúdo dos templates pode ser externalizado depois para configuração ou arquivos de recurso.
8. **Política de senha**: Mínimo de 8 e máximo de 100 caracteres. Não há regras adicionais de complexidade além do comprimento, em linha com a validação atual do frontend.
9. **Formato de telefone**: A regex internacional `^\+?[1-9]\d{10,14}$` é aplicada no backend em alinhamento com o contrato OpenAPI.
10. **Refatoração de entidades**: A entidade `User` e o enum `UserRole` existentes precisam ser refatorados para alinhar com o contrato. O campo atual `name` será dividido em `firstName` + `lastName`. O enum `UserRole` será substituído pelo modelo de quatro valores.
11. **Prefixo de path da API**: O `SecurityConfig` atual usa `/api/auth/**`. O contrato define `/auth/**` sem prefixo `/api`. A estratégia de prefixo precisa ser alinhada. Esta spec assume que os paths do contrato são os canônicos e que qualquer prefixo de aplicação, se existir, é transparente para o frontend via proxy reverso ou base path configurado.

---

## Fora de Escopo Desta Especificação

- Provedores de login social/OAuth2
- Endpoints administrativos para gestão de usuários (listar todos, desativar contas, forçar atribuição de papel)
- Autenticação em dois fatores
- Exclusão de conta / remoção de dados para GDPR
- Rate limiting e proteção contra brute force (recomendado como tarefa futura de hardening)
- Trilha de auditoria / histórico de login

---

## Questões em Aberto

Nenhuma. Todas as decisões foram resolvidas por meio das premissas acima. Se a validação de tokens de convite exigir coordenação com `mod-review` ou `mod-registration`, uma especificação futura deverá tratar o contrato de integração entre módulos.

