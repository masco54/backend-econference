# Plano de Implementação: JWT via Cookie HttpOnly — Backend Spring Boot

**Branch**: `004-jwt-cookie-back` | **Date**: 2026-07-13 | **Spec**: [spec.md](spec.md)
**Input**: Especificação da funcionalidade em `/specs/004-jwt-cookie-back/spec.md`

## Summary

O backend **já implementa** JWT via Cookie HttpOnly. O `AuthController` emite o cookie `auth_token` (HttpOnly, Secure, SameSite=Lax) no login e o limpa no logout. O `JwtAuthenticationFilter` lê o JWT do cookie, valida a assinatura e a sessão ativa no banco. O `SecurityConfig` tem CORS configurado com `allowCredentials=true`. O plano documenta o estado atual, identifica os três gaps menores e define o trabalho de hardening e testes.

## Technical Context

**Language/Version**: Java 24 / Spring Boot 3.4.4  
**Primary Dependencies**: Spring Security 6, Spring Data JPA, JJWT (JwtService)  
**Storage**: PostgreSQL (prod) / H2 (dev) — banco único com schema por módulo; `UserSession` armazena hash do token  
**Testing**: JUnit 5, Spring Boot Test, MockMvc, `@DataJpaTest`  
**Target Platform**: Servidor JVM em container Linux  
**Project Type**: Monólito modular Maven (mod-auth é o único módulo afetado)  
**Performance Goals**: p95 login < 300 ms, p95 /auth/me < 100 ms  
**Constraints**: Sem Authorization header; cookie opaque para o JS; CORS restringe origens explícitas; banco único  
**Architecture Boundaries**: Mudança confinada a `mod-auth`; outros módulos recebem o usuário via `SecurityContext` sem alteração

## Constitution Check

- **Qualidade de Código**: Mudança confinada a `mod-auth` (controller, config). Sem novos acoplamentos entre módulos.
- **Arquitetura Hexagonal**: `AuthController` (adapter-in) → `LoginUseCase`/`LogoutUseCase` (port-in) → `AuthenticationService` (application service). Direção de dependências preservada. `JwtAuthenticationFilter` e `SecurityConfig` são infraestrutura, fora das camadas de domínio/aplicação.
- **Testes**: Testes de integração com MockMvc cobrindo: login emite cookie, GET /auth/me com cookie válido, GET /auth/me sem cookie → 401, logout → cookie Max-Age=0.
- **Consistência de API**: Nenhum endpoint muda URL, método HTTP ou schema de request/response.
- **Performance**: `GET /auth/me` requer uma query de sessão + uma query de usuário — < 10 ms em banco local. Sem N+1.
- **Arquitetura de Dados**: Schema `auth` é dono de `user_sessions`. Nenhuma migração nova necessária para os gaps identificados.
- **Exceptions**: CSRF está desabilitado (`csrf.disable()`). Aceitável pois SameSite=Lax mitiga a maioria dos vetores CSRF e a aplicação não usa cookies de sessão tradicionais. Risco documentado.

## Project Structure

### Documentation (this feature)

```text
specs/004-jwt-cookie-back/
├── plan.md         ← este arquivo
├── research.md
├── quickstart.md
└── contracts/
```

### Source Code

```text
mod-auth/src/main/java/com/econference/auth/
├── controller/
│   └── AuthController.java         ← gap: SameSite=Strict no cookie de logout
├── config/
│   └── SecurityConfig.java         ← gap: lista de origens por perfil de ambiente
├── security/
│   └── JwtAuthenticationFilter.java ← já correto
└── (nenhuma alteração nas camadas domain/application/port)

mod-auth/src/test/java/com/econference/auth/
└── controller/
    └── AuthControllerIntegrationTest.java  ← criar/expandir
```

**Structure Decision**: Monólito modular Maven. Apenas `mod-auth` é tocado. Alterações de config em `application/src/main/resources/` para externalizar origens CORS por ambiente.

## Phase 0: Research

### Descobertas

**Decisão: SameSite=Lax vs SameSite=Strict**
- Estado atual: `SameSite=Lax`
- Decisão: manter `Lax` para o cookie de sessão; Lax já bloqueia CSRF em requests cross-site não-navegação
- Rationale: `Strict` quebraria fluxos de redirect OAuth/email-link onde o cookie não é enviado na primeira navegação após o redirect
- Alternativa rejeitada: `Strict` — causa falhas nos fluxos de verificação de e-mail e reset de senha

**Decisão: CSRF disabled**
- Estado atual: `csrf.disable()`
- Decisão: manter desabilitado
- Rationale: SameSite=Lax + origem restrita no CORS reduz o risco ao mínimo; reabilitar CSRF com cookie HttpOnly requer sincronização de token com o Angular (já tem `csrfInterceptor` mas sem backend configurado)
- Alternativa futura: habilitar CSRF com `CookieCsrfTokenRepository.withHttpOnlyFalse()` em spec separada

**Decisão: origens CORS**
- Estado atual: `${app.cors.allowed-origins:http://localhost:4200}` — uma única string
- Decisão: suportar múltiplas origens separadas por vírgula via propriedade; já implementado com `.split(",")`
- Gap: falta a propriedade configurada em `application-production.properties`

**Estado atual — o que JÁ funciona:**
- Cookie `auth_token` com `HttpOnly=true, Secure=true, SameSite=Lax` emitido no login ✅
- `JwtAuthenticationFilter` lê apenas do cookie `auth_token` ✅
- Logout emite `Set-Cookie: auth_token=; Max-Age=0` ✅
- `GET /auth/me` retorna `UserResponse` para usuário autenticado ✅
- CORS com `allowCredentials=true` e origens configuráveis ✅
- Sessões armazenadas em `UserSession` com hash SHA-256 do token ✅

**Estado atual — gaps identificados:**
1. Cookie de logout não inclui `Secure=true` (inconsistência com o de login)
2. `application-production.properties` não tem `app.cors.allowed-origins` configurado
3. Testes de integração do `AuthController` para os fluxos de cookie não existem

## Phase 1: Design

### Data Model

Sem mudança de schema. Entidades existentes:
- `UserSession`: `id`, `userId`, `tokenHash` (SHA-256), `expiresAt`, `createdAt`
- `User`: sem alteração

### API Contracts

Sem mudança de contratos. Documentação dos atributos do cookie emitido:

**Login — `POST /api/v1/auth/login`**
```
Response Header:
  Set-Cookie: auth_token=<jwt>; Path=/; Max-Age=604800; HttpOnly; Secure; SameSite=Lax
Response Body: LoginResponse { userId, firstName, lastName, email, roles }
```

**Logout — `POST /api/v1/auth/logout`**
```
Response Header:
  Set-Cookie: auth_token=; Path=/; Max-Age=0; HttpOnly; Secure; SameSite=Lax
Response Status: 204 No Content
```

**GET /api/v1/auth/me**
```
Request: Cookie: auth_token=<jwt>
Response: UserResponse { id, firstName, lastName, email, roles, emailVerified }
Error: 401 se cookie ausente, inválido ou sessão expirada
```

### Mudanças de implementação

**Gap 1: cookie de logout sem Secure**
Em `AuthController.logout()`:
```java
ResponseCookie cookie = ResponseCookie.from("auth_token", "")
    .httpOnly(true)
    .secure(true)          // ← adicionar
    .sameSite("Lax")       // ← adicionar para consistência
    .maxAge(0)
    .path("/")
    .build();
```

**Gap 2: CORS de produção**
Em `application-production.properties` (criar se não existir):
```properties
app.cors.allowed-origins=https://app.econference.com
```

**Gap 3: testes de integração**
Criar `AuthControllerIntegrationTest` com:
- `POST /auth/login` com credenciais válidas → verifica `Set-Cookie` com `HttpOnly`, `Secure`
- `GET /auth/me` com cookie válido → 200 com UserResponse
- `GET /auth/me` sem cookie → 401
- `POST /auth/logout` com cookie → 204, verifica `Max-Age=0`

### Quickstart

```bash
# Subir backend
cd back-econference
./mvnw spring-boot:run -pl application

# Testar login e cookie via curl
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@test.com","password":"senha123"}' -v

# Testar GET /auth/me com cookie
curl -b cookies.txt http://localhost:8080/api/v1/auth/me

# Testar logout
curl -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/v1/auth/logout -v

# Executar testes de integração
./mvnw test -pl mod-auth
```
