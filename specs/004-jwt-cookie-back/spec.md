# Especificação de Funcionalidade: JWT via Cookie HttpOnly — Backend Spring Boot

**Feature Branch**: `004-jwt-cookie-back`
**Created**: 2026-07-13
**Status**: Draft
**Input**: "vamos voltar para JWT via Cookie HttpOnly. preciso criar duas specs uma de front e outra de back."

## User Scenarios & Testing *(obrigatório)*

### User Story 1 - Login emite JWT como Cookie HttpOnly (Priority: P1)

Quando o usuário faz login com credenciais válidas, o backend gera um JWT e o retorna como cookie `HttpOnly; Secure; SameSite=Strict`, sem expor o token no corpo da resposta.

**Why this priority**: É o núcleo da feature. Sem isso, nenhum outro cenário funciona.

**Independent Test**: Chamar `POST /api/v1/auth/login` com credenciais válidas e inspecionar o header `Set-Cookie` da resposta; verificar que o cookie contém o JWT e os atributos de segurança corretos.

**Acceptance Scenarios**:

1. **Given** credenciais válidas, **When** `POST /api/v1/auth/login`, **Then** a resposta inclui `Set-Cookie: jwt=<token>; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=<duration>`.
2. **Given** o login bem-sucedido, **When** o corpo da resposta é inspecionado, **Then** o JWT NÃO aparece no corpo — apenas dados do usuário (id, nome, roles).
3. **Given** credenciais inválidas, **When** `POST /api/v1/auth/login`, **Then** retorna 401 e nenhum cookie é emitido.

---

### User Story 2 - Requisições autenticadas validam o cookie JWT (Priority: P1)

O backend lê o cookie JWT em cada requisição, valida a assinatura e extrai o usuário, sem depender do header `Authorization: Bearer`.

**Why this priority**: É o mecanismo central de autenticação. Sem isso, o cookie não protege nenhum endpoint.

**Independent Test**: Chamar `GET /api/v1/auth/me` com o cookie JWT válido no header `Cookie` e verificar que retorna o usuário autenticado.

**Acceptance Scenarios**:

1. **Given** requisição com cookie JWT válido, **When** qualquer endpoint protegido é acessado, **Then** o Spring Security autentica o usuário a partir do cookie.
2. **Given** requisição sem cookie, **When** endpoint protegido é acessado, **Then** retorna 401.
3. **Given** cookie JWT com assinatura inválida ou expirado, **When** endpoint protegido é acessado, **Then** retorna 401.
4. **Given** cookie válido, **When** `GET /api/v1/auth/me`, **Then** retorna o usuário com id, nome, email e roles.

---

### User Story 3 - Logout invalida a sessão e remove o cookie (Priority: P1)

Quando o usuário faz logout, o backend remove o cookie do browser e invalida a sessão se houver lista de revogação.

**Why this priority**: Logout incompleto deixa o cookie válido — risco de segurança crítico.

**Independent Test**: Chamar `POST /api/v1/auth/logout` com cookie válido e verificar que a resposta contém `Set-Cookie` com `Max-Age=0` e que chamadas subsequentes com o mesmo cookie retornam 401.

**Acceptance Scenarios**:

1. **Given** usuário autenticado, **When** `POST /api/v1/auth/logout`, **Then** a resposta contém `Set-Cookie: jwt=; Max-Age=0; Path=/` para limpar o cookie no browser.
2. **Given** logout concluído, **When** o mesmo cookie é usado em requisição subsequente, **Then** retorna 401.

---

### User Story 4 - CORS configurado para aceitar credenciais do frontend (Priority: P1)

O backend aceita requisições cross-origin com cookies (`withCredentials: true`) apenas da origem legítima do frontend.

**Why this priority**: Sem CORS correto, o browser bloqueia o envio do cookie e a autenticação falha em ambiente de desenvolvimento e produção.

**Independent Test**: Fazer requisição a partir do frontend (`http://localhost:4200`) com `withCredentials: true` e verificar que o header `Access-Control-Allow-Credentials: true` está presente na resposta.

**Acceptance Scenarios**:

1. **Given** requisição originada de `http://localhost:4200` (dev), **When** qualquer endpoint é acessado, **Then** a resposta contém `Access-Control-Allow-Origin: http://localhost:4200` e `Access-Control-Allow-Credentials: true`.
2. **Given** requisição originada de domínio não autorizado, **When** tenta acessar a API, **Then** CORS bloqueia com 403.
3. **Given** configuração de produção, **When** a origem do frontend de produção acessa a API, **Then** CORS permite com credenciais.

---

### Edge Cases

- O que acontece quando o módulo `mod-auth` não encontra o cookie mas o header `Authorization` está presente? → Por segurança, o header deve ser ignorado após a migração para cookies.
- O que acontece quando o token expira durante uma sessão longa? → Retornar 401; o frontend trata redirecionando para login.
- O que acontece quando a requisição vem de um client não-browser (Postman, testes)? → Deve funcionar enviando o cookie no header `Cookie` manualmente.
- O que acontece quando a aplicação roda em HTTP (desenvolvimento sem HTTPS)? → O atributo `Secure` pode ser omitido apenas em perfil de desenvolvimento; em produção é obrigatório.
- O que acontece quando há múltiplas instâncias do backend (load balancer)? → O JWT é stateless; não há problema de sessão distribuída, desde que todas as instâncias compartilhem a mesma chave de assinatura.

## Requirements *(obrigatório)*

### Functional Requirements

- **FR-001**: O `mod-auth` MUST emitir o JWT como cookie `HttpOnly; Path=/; SameSite=Strict` na resposta de login bem-sucedido, com `Secure` ativado em perfis não-development.
- **FR-002**: O filtro de autenticação do Spring Security MUST ler o JWT do cookie (não do header `Authorization`) para autenticar requisições.
- **FR-003**: O endpoint `POST /api/v1/auth/logout` MUST remover o cookie emitindo `Set-Cookie` com `Max-Age=0`.
- **FR-004**: O endpoint `GET /api/v1/auth/me` MUST retornar os dados do usuário autenticado (id, firstName, lastName, email, roles) a partir do cookie válido.
- **FR-005**: A configuração CORS MUST permitir `withCredentials: true` somente para origens explicitamente configuradas (não `*`); a origem do frontend deve ser configurável por ambiente.
- **FR-006**: O `mod-auth` MUST ser o único módulo dono do filtro JWT e da lógica de emissão/validação de cookies; outros módulos recebem o usuário autenticado via `SecurityContext`.
- **FR-007**: O `Max-Age` do cookie MUST ser configurável (default: 8 horas para sessão normal), alinhado com a expectativa do frontend.
- **FR-008**: O sistema MUST preservar a compatibilidade dos endpoints existentes (`/auth/login`, `/auth/logout`, `/auth/me`, `/auth/register`) — apenas o mecanismo de transporte do token muda.
- **FR-009**: Testes de integração MUST cobrir: login emite cookie, requisição com cookie válido é autenticada, requisição sem cookie retorna 401, logout limpa cookie.
- **FR-010**: O sistema MUST adheir à arquitetura de módulos existente — a lógica de cookie fica no `mod-auth` sem vazar para outros módulos.

### Key Entities

- **JwtCookieFilter**: Filtro Spring Security que lê o JWT do cookie e autentica a requisição.
- **AuthController** (mod-auth): Endpoints de login/logout/me; responsável por emitir e limpar o cookie.
- **JwtService** (mod-auth): Gera e valida JWTs; sem alteração de interface, apenas o transporte muda.
- **CorsConfig**: Configuração global de CORS com origens permitidas por perfil.

## Success Criteria *(obrigatório)*

### Measurable Outcomes

- **SC-001**: Login retorna cookie com todos os atributos de segurança (`HttpOnly`, `Secure` em prod, `SameSite=Strict`) — verificável via header `Set-Cookie`.
- **SC-002**: 100% das requisições autenticadas validam o JWT do cookie; zero dependência do header `Authorization` após a migração.
- **SC-003**: Logout garante que o cookie seja removido do browser — verificável via `Max-Age=0` na resposta.
- **SC-004**: Testes de integração do `mod-auth` passam cobrindo os 4 cenários obrigatórios (login, autenticação, 401 sem cookie, logout).
- **SC-005**: Nenhum endpoint existente tem sua URL, método HTTP ou contrato de request/response alterado — apenas o mecanismo de sessão muda.
