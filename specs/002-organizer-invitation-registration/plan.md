# Plano de Implementação: Cadastro de Organizador via Link de Ativação após Aprovação de Evento

**Branch**: `002-organizer-activation-registration` | **Date**: 2026-07-11 | **Spec**: [spec.md](spec.md)  
**Input**: Especificação da funcionalidade em `/specs/002-organizer-invitation-registration/spec.md`

## Summary

Implementar um fluxo de cadastro de organizador acionado automaticamente quando o administrador aprova uma solicitação de evento. Ao aprovar, `mod-event` aciona um novo contrato publicado em `shared` (`OrganizerActivationTokenPort`) que `mod-auth` implementa — gerando um `OrganizerActivationToken` com os dados do solicitante (firstName, lastName, email, phone, eventId) e disparando o e-mail de ativação. O solicitante clica no link, valida o token via `GET /api/v1/auth/organizer-activation/validate` (que retorna os dados pré-preenchidos) e conclui o cadastro via `POST /api/v1/auth/organizer-activation/complete` informando apenas senha e, opcionalmente, telefone. A conta é criada com `emailVerified = true` e roles `[ORGANIZER, PARTICIPANT]`, sem verificação adicional de e-mail.

A abordagem move a responsabilidade de gerenciamento do token de ativação para `mod-auth` (dono da autenticação), enquanto `mod-event` permanece dono da aprovação da solicitação. O padrão segue exatamente a `EventRequestAccountProvisioningPort` já existente em `shared/`.

## Technical Context

**Language/Version**: Java 24  
**Primary Dependencies**: Spring Boot 3.4, Spring Security, Spring Data JPA, Jakarta Validation, Lombok, Spring Mail (Thymeleaf templates)  
**Storage**: PostgreSQL (produção) + H2 (testes locais); banco físico único com separação por schema por módulo  
**Testing**: JUnit 5, Spring Boot Test, MockMvc, Mockito, H2 em memória  
**Target Platform**: Servidor JVM em container Linux  
**Project Type**: Monólito modular backend (Maven multi-módulo)  
**Performance Goals**: p95 < 200 ms para `/validate` (leitura simples), p95 < 500 ms para `/complete` (escrita + e-mail assíncrono)  
**Constraints**: Preservar fronteiras de módulo; `mod-auth` MUST NOT conhecer internos de `mod-event`; acoplamento apenas via contratos em `shared`; banco único com schema por módulo; sem N+1; `RegisterRequest` existente não deve ser alterado  
**Scale/Scope**: 2 módulos alterados (`mod-auth`, `mod-event`), 1 módulo de contrato (`shared`), ~6 novos arquivos Java em `mod-auth`, 1 novo contrato em `shared`, 1 adapter de saída novo em `mod-event`, 1 tabela nova, 1 template de e-mail novo  
**Architecture Boundaries**:
- `shared/`: novo `OrganizerActivationTokenPort` (interface publicada)
- `mod-auth/domain`: nova entidade `OrganizerActivationToken`
- `mod-auth/application/port/in`: `CreateOrganizerActivationTokenUseCase`, `ValidateOrganizerActivationTokenUseCase`, `CompleteOrganizerActivationUseCase`
- `mod-auth/application/port/out`: `OrganizerActivationTokenRepositoryPort`
- `mod-auth/adapter/in/web`: `OrganizerActivationController` (novos endpoints)
- `mod-auth/adapter/out/identity`: `OrganizerActivationTokenCreationAdapter` (implementa `OrganizerActivationTokenPort` de `shared`)
- `mod-event/adapter/out`: novo adapter que chama `OrganizerActivationTokenPort` ao aprovar uma solicitação
- `application/`: composition root recebe o novo wiring

**Data Architecture**: Tabela `organizer_activation_tokens` criada no schema do `mod-auth` (mesmo schema da tabela `users`, `email_verification_tokens`, `password_reset_tokens`). Migration via Flyway/Liquibase. `mod-event` nunca acessa essa tabela diretamente — usa apenas o contrato publicado em `shared`.

## Constitution Check

*GATE: Passou antes da Fase 0 de pesquisa. Revalidado após a Fase 1 de design.*

| Princípio | Status | Notas |
|-----------|--------|-------|
| **I. Qualidade de Código** | ✅ PASS | `mod-auth` é o único dono de `OrganizerActivationToken`. `mod-event` usa apenas `OrganizerActivationTokenPort` de `shared`. Nenhum vazamento entre módulos. Contratos explícitos, responsabilidade única por classe. |
| **II. Arquitetura Hexagonal/Clean** | ✅ PASS | Domínio (`OrganizerActivationToken`) → Application services (use cases) → Ports (in/out) → Adapters (web controller, JPA repository, email). Direção de dependências aponta para dentro. `mod-event` acoplado apenas à interface em `shared`. |
| **III. Testes** | ✅ PASS | Unitários: `CompleteOrganizerActivationService`, `ValidateOrganizerActivationTokenService`. Integração: `OrganizerActivationController` via MockMvc. Contrato: teste de contrato para `OrganizerActivationTokenPort` (mesmo padrão de `EventRequestAccountProvisioningPortContractTest`). Regressão: token reutilizado rejeitado. |
| **IV. Consistência UX/API** | ✅ PASS | Endpoints seguem convenção `/api/v1/auth/*`. Semântica HTTP: `GET` para validate, `POST` para complete, `201` para criação de conta, `409` para e-mail duplicado, `422` para validação. Padrão de resposta consistente com `GlobalExceptionHandler` existente. |
| **V. Performance** | ✅ PASS | `/validate`: 1 SELECT por token (índice único em `token`). `/complete`: 1 SELECT + 1 INSERT + e-mail assíncrono (via `@Async`). Sem N+1. E-mail fora do caminho crítico da transação. |
| **VI. Arquitetura de Dados** | ✅ PASS | Banco físico único confirmado. Tabela `organizer_activation_tokens` no schema do `mod-auth`. Nenhum acesso de `mod-event` ao schema de `mod-auth`. Migração documentada na `quickstart.md`. Rollback: DROP TABLE. |
| **VII. Exceptions** | ✅ NENHUMA | Nenhuma violação de princípio identificada. |

## Project Structure

### Documentation (this feature)

```text
specs/002-organizer-invitation-registration/
├── plan.md              # Este arquivo
├── spec.md              # Especificação da funcionalidade
├── research.md          # Decisões da Fase 0
├── data-model.md        # Entidade OrganizerActivationToken e relações
├── quickstart.md        # Guia de desenvolvimento local e testes
├── contracts/
│   └── organizer-activation.openapi.yaml  # OpenAPI 3.0 dos novos endpoints
└── tasks.md             # Gerado por /speckit.tasks (não criado aqui)
```

### Source Code (repository root)

```text
shared/
└── src/main/java/com/econference/shared/
    └── contract/
        └── OrganizerActivationTokenPort.java          # NOVO: contrato publicado

mod-auth/
├── src/main/java/com/econference/auth/
│   ├── domain/
│   │   └── OrganizerActivationToken.java              # NOVO: entidade JPA
│   ├── application/
│   │   ├── port/in/
│   │   │   ├── CreateOrganizerActivationTokenUseCase.java  # NOVO
│   │   │   ├── ValidateOrganizerActivationTokenUseCase.java # NOVO
│   │   │   └── CompleteOrganizerActivationUseCase.java      # NOVO
│   │   ├── port/out/
│   │   │   └── OrganizerActivationTokenRepositoryPort.java  # NOVO
│   │   └── service/
│   │       ├── OrganizerActivationTokenService.java         # NOVO: orquestra criação e validação
│   │       └── CompleteOrganizerActivationService.java      # NOVO: orquestra conclusão do cadastro
│   ├── adapter/
│   │   ├── in/web/
│   │   │   ├── OrganizerActivationController.java           # NOVO
│   │   │   └── dto/
│   │   │       ├── request/
│   │   │       │   └── CompleteOrganizerActivationRequest.java  # NOVO
│   │   │       └── response/
│   │   │           ├── ValidateOrganizerActivationTokenResponse.java  # NOVO
│   │   │           └── CompleteOrganizerActivationResponse.java       # NOVO
│   │   └── out/
│   │       ├── identity/
│   │       │   └── OrganizerActivationTokenCreationAdapter.java  # NOVO: implementa OrganizerActivationTokenPort
│   │       └── persistence/
│   │           ├── OrganizerActivationTokenJpaRepository.java    # NOVO
│   │           └── OrganizerActivationTokenRepositoryAdapter.java # NOVO
│   └── config/
│       └── OrganizerActivationConfig.java                        # NOVO: @ConfigurationProperties
├── src/main/resources/templates/email/
│   └── organizer-activation-email.html                          # NOVO: template Thymeleaf
└── src/test/java/com/econference/auth/
    ├── application/service/
    │   ├── OrganizerActivationTokenServiceTest.java              # NOVO
    │   └── CompleteOrganizerActivationServiceTest.java           # NOVO
    └── adapter/in/web/
        └── OrganizerActivationControllerTest.java                # NOVO

mod-event/
└── src/main/java/com/econference/event/
    └── adapter/out/
        └── activation/
            └── OrganizerActivationTokenEventAdapter.java         # NOVO: chama OrganizerActivationTokenPort ao aprovar
            (modificação em ApproveEventRequestService para injetar e chamar o adapter)

application/
└── src/main/java/
    └── (modificação no composition root para wiring do OrganizerActivationTokenCreationAdapter)
```

**Structure Decision**: `mod-auth` é o dono exclusivo da entidade `OrganizerActivationToken`, dos use cases de validação e conclusão de cadastro, e dos endpoints HTTP. O contrato `OrganizerActivationTokenPort` é publicado em `shared/` para que `mod-event` possa acionar a criação do token após aprovar uma solicitação — seguindo exatamente o padrão inverso já existente (`EventRequestAccountProvisioningPort`, que permite `mod-event` chamar `mod-auth` para provisionar contas). `mod-event` implementa apenas um adapter de saída que delega ao contrato; nenhum internos do `mod-auth` é exposto. O schema `mod-auth` recebe a nova tabela via migração versionada.

## Complexity Tracking

> Nenhuma violação de princípio identificada. Tabela abaixo omitida conforme instrução do template.
