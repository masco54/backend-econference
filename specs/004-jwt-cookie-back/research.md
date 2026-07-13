# Research: JWT via Cookie HttpOnly — Backend Spring Boot

**Feature**: 004-jwt-cookie-back | **Date**: 2026-07-13

## Estado atual verificado

### O que já funciona ✅

| Componente | Status | Detalhe |
|---|---|---|
| `AuthController.login()` | ✅ Completo | Emite `Set-Cookie: auth_token=<jwt>; HttpOnly; Secure; SameSite=Lax` |
| `AuthController.logout()` | ✅ Parcial | Emite `Max-Age=0` mas sem `Secure` e `SameSite` no cookie de limpeza |
| `AuthController.getCurrentUser()` | ✅ Completo | `GET /auth/me` retorna `UserResponse` para usuário autenticado |
| `JwtAuthenticationFilter` | ✅ Completo | Lê `auth_token` do cookie; valida JWT + sessão ativa |
| `SecurityConfig` CORS | ✅ Completo | `allowCredentials=true`, origens configuráveis via `app.cors.allowed-origins` |
| Sem `Authorization: Bearer` | ✅ Completo | O filtro ignora o header; lê apenas do cookie |
| `UserSession` com hash SHA-256 | ✅ Completo | Tokens armazenados como hash para invalidação |

### Gaps identificados ❌

| Gap | Impacto | Solução |
|---|---|---|
| Cookie de logout sem `Secure` e `SameSite` | Inconsistência entre login e logout; comportamento pode diferir em prod | Adicionar `.secure(true).sameSite("Lax")` no cookie de logout |
| `application-production.properties` sem `app.cors.allowed-origins` | CORS usa o default `localhost:4200` em produção | Adicionar propriedade com origem de produção |
| Sem testes de integração para fluxo de cookie | Regressões não detectadas | Criar `AuthControllerIntegrationTest` |

## Decisões de design

### SameSite=Lax vs SameSite=Strict

- **Decisão**: manter `SameSite=Lax`
- **Rationale**: `Strict` quebra fluxos onde o usuário chega via link de e-mail (verificação, reset de senha) — o cookie não é enviado na primeira navegação após o redirect externo.
- **Alternativa rejeitada**: `Strict` — impacto direto nos fluxos de verificação de e-mail e reset de senha.

### CSRF desabilitado

- **Decisão**: manter `csrf.disable()` por ora
- **Rationale**: SameSite=Lax + CORS restrito à origem do front mitigam os principais vetores. Reabilitar CSRF requer sincronização de token duplo com o Angular — escopo de spec separada.
- **Risco aceito**: documentado no Constitution Check do plan.md.

### Sem revogação de Authorization header

- **Decisão**: não aceitar `Authorization: Bearer` após a migração
- **Rationale**: o `JwtAuthenticationFilter` já só lê o cookie; não é necessária ação para bloquear o header.

## Referências

- [AuthController.java](../../../mod-auth/src/main/java/com/econference/auth/controller/AuthController.java)
- [JwtAuthenticationFilter.java](../../../mod-auth/src/main/java/com/econference/auth/security/JwtAuthenticationFilter.java)
- [SecurityConfig.java](../../../mod-auth/src/main/java/com/econference/auth/config/SecurityConfig.java)
