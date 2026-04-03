# Tarefas: Módulo de Autenticação (mod-auth)

**Entrada**: Documentos de design em `specs/master/plan.md`, `specs/master/spec.md`, `specs/master/contracts/auth.openapi.yaml` e `specs/master/contracts/auth-contract.md`  
**Local de Saída**: `specs/master/tasks.md`  
**Pré-requisitos**: `specs/master/plan.md` e `specs/master/spec.md` são os artefatos canônicos de design do auth

**Testes**: Testes automatizados são obrigatórios para esta funcionalidade porque a implementação altera autenticação, persistência, contratos, segurança e fronteiras entre módulos.

**Organização**: As tarefas são agrupadas por user story para que cada incremento possa ser implementado e validado independentemente após a conclusão da base fundacional.

## Fase 1: Setup (Infraestrutura Compartilhada)

**Objetivo**: Preparar dependências do módulo, propriedades de runtime e estrutura de pacotes para a implementação do auth.

- [X] T001 Atualizar `mod-auth/pom.xml` com as dependências de mail, validation, spring-security-test e Testcontainers exigidas pelo plano do auth
- [X] T002 [P] Atualizar `application/pom.xml` e o `pom.xml` raiz com o wiring de teste/runtime necessário para os testes de integração do auth
- [X] T003 [P] Adicionar propriedades de auth, JWT, cookie, mail e origem do frontend em `application/src/main/resources/application.yml`
- [X] T004 [P] Adicionar overrides de teste do auth para configurações de JWT, mail e datasource em `application/src/test/resources/application-test.yml`
- [X] T005 [P] Expandir o esqueleto de pacotes em `mod-auth/src/main/java/com/econference/auth/package-info.java` e criar marcadores de pacote em `mod-auth/src/main/java/com/econference/auth/application/`, `adapter/`, `config/` e `security/`

---

## Fase 2: Fundacional (Pré-requisitos Bloqueantes)

**Objetivo**: Entregar o schema, os contratos compartilhados e a infraestrutura dos quais todas as user stories dependem.

**⚠️ CRÍTICO**: Nenhuma user story deve começar antes da conclusão desta fase.

- [X] T006 Criar a migração de refatoração do schema de auth em `application/src/main/resources/db/migration/V2__refactor_auth_schema.sql`
- [X] T007 [P] Refatorar `mod-auth/src/main/java/com/econference/auth/domain/User.java` e `mod-auth/src/main/java/com/econference/auth/domain/UserRole.java` para nomes separados, hash de senha, verificação de email e suporte a múltiplos papéis
- [X] T008 [P] Criar `mod-auth/src/main/java/com/econference/auth/domain/EmailVerificationToken.java`, `mod-auth/src/main/java/com/econference/auth/domain/PasswordResetToken.java` e `mod-auth/src/main/java/com/econference/auth/domain/UserSession.java`
- [X] T009 [P] Criar repositórios Spring Data em `mod-auth/src/main/java/com/econference/auth/repository/UserRepository.java`, `EmailVerificationTokenRepository.java`, `PasswordResetTokenRepository.java` e `UserSessionRepository.java`
- [X] T010 [P] Criar ports de repositório de saída em `mod-auth/src/main/java/com/econference/auth/application/port/out/UserRepositoryPort.java`, `EmailVerificationTokenRepositoryPort.java`, `PasswordResetTokenRepositoryPort.java` e `UserSessionRepositoryPort.java`
- [X] T011 [P] Implementar adaptadores de persistência em `mod-auth/src/main/java/com/econference/auth/adapter/out/persistence/UserRepositoryAdapter.java`, `EmailVerificationTokenRepositoryAdapter.java`, `PasswordResetTokenRepositoryAdapter.java` e `UserSessionRepositoryAdapter.java`
- [X] T012 [P] Adicionar `mod-auth/src/main/java/com/econference/auth/config/JwtConfig.java`, `AsyncConfig.java` e o contrato base `EmailSenderPort.java` em `mod-auth/src/main/java/com/econference/auth/application/port/out/EmailSenderPort.java`
- [X] T013 Implementar a baseline de segurança com credenciais em `mod-auth/src/main/java/com/econference/auth/config/SecurityConfig.java`, incluindo regras de rotas públicas, encaixe do filtro de cookie auth, origens de CORS e exceções de H2/health
- [X] T014 [P] Adicionar exceções compartilhadas de auth em `shared/src/main/java/com/econference/shared/exception/AuthException.java` e `ForbiddenException.java`
- [X] T015 Atualizar `shared/src/main/java/com/econference/shared/exception/GlobalExceptionHandler.java` para emitir payloads compatíveis com o contrato (`message`, `code`, `details`) para erros de validação, auth, forbidden e negócio
- [X] T016 [P] Criar o contrato de identidade entre módulos em `shared/src/main/java/com/econference/shared/contract/UserIdentityPort.java`
- [ ] T017 [P] Adicionar um teste de regressão de fronteira de módulo em `mod-auth/src/test/java/com/econference/auth/architecture/AuthModuleArchitectureTest.java`

**Checkpoint**: Schema, contratos compartilhados, baseline de segurança e infraestrutura de persistência estão prontos para o trabalho das histórias.

---

## Fase 3: User Story 1 - Cadastro de Usuário e Verificação de Email (Prioridade: P1) 🎯 MVP

**Objetivo**: Permitir que um visitante se cadastre, receba email de verificação, verifique a conta e solicite um novo email de verificação.

**Teste Independente**: Um novo usuário consegue se cadastrar via `POST /auth/register`, verificar via `POST /auth/verify-email` e solicitar um novo token via `POST /auth/resend-verification`, enquanto cenários de email duplicado, validação e token expirado retornam os erros definidos no contrato.

### Testes da User Story 1

- [ ] T018 [P] [US1] Adicionar cobertura de integração do endpoint de cadastro em `mod-auth/src/test/java/com/econference/auth/adapter/in/web/AuthRegistrationControllerIT.java`
- [ ] T019 [P] [US1] Adicionar cobertura de integração dos endpoints de verificação e reenvio em `mod-auth/src/test/java/com/econference/auth/adapter/in/web/AuthEmailVerificationControllerIT.java`
- [ ] T020 [P] [US1] Adicionar testes unitários do serviço de cadastro em `mod-auth/src/test/java/com/econference/auth/application/service/RegistrationServiceTest.java`
- [ ] T021 [P] [US1] Adicionar testes de persistência do token de verificação em `mod-auth/src/test/java/com/econference/auth/adapter/out/persistence/EmailVerificationTokenRepositoryAdapterIT.java`
- [ ] T022 [P] [US1] Adicionar validações do contrato auth para os fluxos de cadastro em `mod-auth/src/test/java/com/econference/auth/contract/AuthRegistrationContractTest.java`

### Implementação da User Story 1

- [X] T023 [P] [US1] Criar `RegisterRequest.java` e `RegisterResponse.java` em `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/request/` e `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/response/`
- [X] T024 [P] [US1] Criar `VerifyEmailRequest.java` e `ResendVerificationRequest.java` em `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/request/`
- [X] T025 [P] [US1] Criar ports de entrada em `mod-auth/src/main/java/com/econference/auth/application/port/in/RegisterUserUseCase.java`, `VerifyEmailUseCase.java` e `ResendVerificationUseCase.java`
- [X] T026 [P] [US1] Implementar o stub de convite em `mod-auth/src/main/java/com/econference/auth/adapter/out/stub/InvitationTokenStubAdapter.java`
- [X] T027 [P] [US1] Implementar o envio de email em `mod-auth/src/main/java/com/econference/auth/adapter/out/email/SpringMailEmailSenderAdapter.java` e `EmailTemplates.java`
- [X] T028 [US1] Implementar a orquestração de cadastro em `mod-auth/src/main/java/com/econference/auth/application/service/RegistrationService.java`
- [X] T029 [US1] Implementar a orquestração de verificação de token em `mod-auth/src/main/java/com/econference/auth/application/service/EmailVerificationService.java`
- [X] T030 [US1] Expandir `mod-auth/src/main/java/com/econference/auth/controller/AuthController.java` com `/auth/register`, `/auth/verify-email` e `/auth/resend-verification`

**Checkpoint**: Cadastro de visitante e verificação de email funcionam de ponta a ponta e podem ser testados de forma independente.

---

## Fase 4: User Story 2 - Login, Logout e Usuário Atual da Sessão (Prioridade: P1)

**Objetivo**: Permitir que um usuário verificado faça login, receba o cookie `auth_token`, obtenha o perfil atual e faça logout com segurança.

**Teste Independente**: Uma conta verificada consegue fazer login via `POST /auth/login`, receber um cookie HttpOnly, chamar `GET /auth/me` e invalidar a sessão via `POST /auth/logout`; credenciais inválidas e contas não verificadas retornam os 401 esperados.

### Testes da User Story 2

- [ ] T031 [P] [US2] Adicionar testes de integração de login, logout e usuário atual em `mod-auth/src/test/java/com/econference/auth/adapter/in/web/AuthSessionControllerIT.java`
- [ ] T032 [P] [US2] Adicionar testes unitários do serviço JWT e do filtro de autenticação em `mod-auth/src/test/java/com/econference/auth/security/JwtAuthenticationFilterTest.java`
- [ ] T033 [P] [US2] Adicionar testes de integração de cookie, CORS e acesso não autorizado em `mod-auth/src/test/java/com/econference/auth/config/SecurityConfigIT.java`
- [ ] T034 [P] [US2] Adicionar testes de persistência de sessão em `mod-auth/src/test/java/com/econference/auth/adapter/out/persistence/UserSessionRepositoryAdapterIT.java`
- [ ] T035 [P] [US2] Adicionar validações do contrato auth para login/logout/me em `mod-auth/src/test/java/com/econference/auth/contract/AuthSessionContractTest.java`

### Implementação da User Story 2

- [X] T036 [P] [US2] Criar `LoginRequest.java`, `LoginResponse.java` e `UserResponse.java` em `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/request/` e `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/response/`
- [X] T037 [P] [US2] Criar ports de entrada em `mod-auth/src/main/java/com/econference/auth/application/port/in/LoginUseCase.java`, `LogoutUseCase.java` e `GetCurrentUserUseCase.java`
- [X] T038 [P] [US2] Criar `UserMapper.java` em `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/mapper/UserMapper.java`
- [X] T039 [US2] Implementar `JwtService.java`, `AuthenticatedUserPrincipal.java` e `JwtAuthenticationFilter.java` em `mod-auth/src/main/java/com/econference/auth/security/`
- [X] T040 [US2] Implementar a orquestração de sessão e login em `mod-auth/src/main/java/com/econference/auth/application/service/AuthenticationService.java`
- [X] T041 [US2] Finalizar emissão de cookie, invalidação de sessão e wiring do filtro auth em `mod-auth/src/main/java/com/econference/auth/config/SecurityConfig.java`
- [X] T042 [US2] Expandir `mod-auth/src/main/java/com/econference/auth/controller/AuthController.java` com `/auth/login`, `/auth/logout` e `/auth/me`

**Checkpoint**: A autenticação de sessão baseada em cookie funciona de forma independente para usuários verificados.

---

## Fase 5: User Story 3 - Recuperação e Alteração de Senha (Prioridade: P2)

**Objetivo**: Permitir que usuários solicitem recuperação de senha, redefinam a senha com token e alterem a senha enquanto autenticados.

**Teste Independente**: `POST /auth/password/forgot`, `POST /auth/password/reset` e `POST /auth/password/change` cumprem as regras de não enumeração de conta, expiração de token e invalidação de sessão sem depender de funcionalidades de perfil ou usuário público.

### Testes da User Story 3

- [ ] T043 [P] [US3] Adicionar testes de integração de forgot e reset de senha em `mod-auth/src/test/java/com/econference/auth/adapter/in/web/AuthPasswordRecoveryControllerIT.java`
- [ ] T044 [P] [US3] Adicionar testes de integração de alteração de senha em `mod-auth/src/test/java/com/econference/auth/adapter/in/web/AuthPasswordChangeControllerIT.java`
- [ ] T045 [P] [US3] Adicionar testes unitários do serviço de senha em `mod-auth/src/test/java/com/econference/auth/application/service/PasswordServiceTest.java`
- [ ] T046 [P] [US3] Adicionar testes de persistência de token de reset e invalidação de sessão em `mod-auth/src/test/java/com/econference/auth/adapter/out/persistence/PasswordResetTokenRepositoryAdapterIT.java`
- [ ] T047 [P] [US3] Adicionar validações do contrato auth para os fluxos de senha em `mod-auth/src/test/java/com/econference/auth/contract/AuthPasswordContractTest.java`

### Implementação da User Story 3

- [X] T048 [P] [US3] Criar `ForgotPasswordRequest.java`, `ResetPasswordRequest.java` e `ChangePasswordRequest.java` em `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/request/`
- [X] T049 [P] [US3] Criar ports de entrada em `mod-auth/src/main/java/com/econference/auth/application/port/in/ForgotPasswordUseCase.java`, `ResetPasswordUseCase.java` e `ChangePasswordUseCase.java`
- [X] T050 [US3] Implementar a orquestração do ciclo de vida de senha em `mod-auth/src/main/java/com/econference/auth/application/service/PasswordService.java`
- [X] T051 [US3] Estender a renderização de email de reset de senha em `mod-auth/src/main/java/com/econference/auth/adapter/out/email/EmailTemplates.java` e `SpringMailEmailSenderAdapter.java`
- [X] T052 [US3] Expandir `mod-auth/src/main/java/com/econference/auth/controller/AuthController.java` com `/auth/password/forgot`, `/auth/password/reset` e `/auth/password/change`

**Checkpoint**: Recuperação e alteração de senha funcionam de forma independente e segura.

---

## Fase 6: User Story 4 - Dados Públicos do Usuário, Atualização de Perfil e Papéis (Prioridade: P2)

**Objetivo**: Permitir que usuários autenticados leiam perfis públicos, atualizem o próprio perfil e listem seus papéis ativos.

**Teste Independente**: Um usuário autenticado consegue chamar `GET /users/{userId}`, `PATCH /users/{userId}` e `GET /users/me/roles`; respostas públicas omitem campos privados, as regras de atualização do próprio perfil são aplicadas e tentativas sem autenticação ou em nome de outro usuário falham com os status corretos.

### Testes da User Story 4

- [ ] T053 [P] [US4] Adicionar testes de integração de perfil de usuário e papéis em `mod-auth/src/test/java/com/econference/auth/adapter/in/web/UserControllerIT.java`
- [ ] T054 [P] [US4] Adicionar testes unitários do serviço de perfil de usuário em `mod-auth/src/test/java/com/econference/auth/application/service/UserProfileServiceTest.java`
- [ ] T055 [P] [US4] Adicionar testes do contrato de identidade entre módulos em `mod-auth/src/test/java/com/econference/auth/contract/UserIdentityPortContractTest.java`
- [ ] T056 [P] [US4] Adicionar validações do contrato auth para endpoints de usuário público e perfil em `mod-auth/src/test/java/com/econference/auth/contract/UserApiContractTest.java`

### Implementação da User Story 4

- [X] T057 [P] [US4] Criar `UpdateProfileRequest.java`, `PublicUserResponse.java` e `RolesResponse.java` em `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/request/` e `mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/response/`
- [X] T058 [P] [US4] Criar ports de entrada em `mod-auth/src/main/java/com/econference/auth/application/port/in/GetPublicUserUseCase.java`, `UpdateProfileUseCase.java` e `GetMyRolesUseCase.java`
- [X] T059 [US4] Implementar a orquestração de perfil e papéis em `mod-auth/src/main/java/com/econference/auth/application/service/UserProfileService.java`
- [X] T060 [US4] Adicionar o adaptador HTTP em `mod-auth/src/main/java/com/econference/auth/adapter/in/web/UserController.java`
- [X] T061 [US4] Implementar o adaptador de identidade entre módulos em `mod-auth/src/main/java/com/econference/auth/adapter/out/identity/AuthUserIdentityAdapter.java`

**Checkpoint**: Leitura/atualização de perfil e listagem de papéis funcionam de forma independente para usuários autenticados.

---

## Fase 7: Polimento e Preocupações Transversais

**Objetivo**: Concluir documentação, validação de migração e hardening em todas as histórias.

- [X] T062 [P] Atualizar `README.md` e `specs/master/contracts/auth-contract.md` com o comportamento final do fluxo de auth, requisitos de cookie e notas de integração local
- [X] T063 [P] Criar `specs/master/quickstart.md` com passos manuais de verificação para cadastro, login, reset de senha e gerenciamento de perfil
- [ ] T064 Adicionar cobertura de regressão de migração em `mod-auth/src/test/java/com/econference/auth/adapter/out/persistence/AuthSchemaMigrationIT.java`
- [X] T065 Reforçar a consistência dos códigos de exceção e dos payloads de validação em `shared/src/main/java/com/econference/shared/exception/GlobalExceptionHandler.java` e `mod-auth/src/main/java/com/econference/auth/controller/AuthController.java`
- [X] T066 Executar os comandos Maven de verificação afetados e registrar a matriz de testes do módulo auth em `specs/master/checklists/requirements.md`

---

## Dependências e Ordem de Execução

### Dependências entre Fases

- **Fase 1 - Setup**: Sem dependências; pode começar imediatamente
- **Fase 2 - Fundacional**: Depende da Fase 1; bloqueia todas as user stories
- **Fase 3 - US1**: Depende da Fase 2; ponto de início recomendado para o MVP
- **Fase 4 - US2**: Depende da Fase 2; pode avançar após o início da US1 porque compartilha o mesmo schema fundacional e a mesma camada de segurança
- **Fase 5 - US3**: Depende da Fase 2; idealmente executada após a US2, pois reutiliza o comportamento de invalidação de sessão autenticada
- **Fase 6 - US4**: Depende da Fase 2; pode ser executada em paralelo com a US3 quando o modelo de usuário compartilhado e o mapper estiverem estáveis
- **Fase 7 - Polimento**: Depende da conclusão das user stories escolhidas para entrega

### Dependências entre User Stories

- **US1**: Não depende de outras user stories após a fase fundacional; este é o slice de MVP
- **US2**: Não depende estritamente da US1, mas se beneficia das mesmas fixtures de usuário e convenções de mapper
- **US3**: Reutiliza a lógica de invalidação de sessão introduzida na US2; por isso a sequência US2 → US3 é preferível
- **US4**: Reutiliza o modelo de usuário e o contexto de auth, mas continua testável de forma independente após a fase fundacional

### Dentro de Cada User Story

- Escrever primeiro os testes listados e confirmar que falham contra o esqueleto atual
- Concluir DTOs e ports antes das implementações de serviço
- Concluir serviços antes do wiring dos controllers
- Finalizar as validações de persistência, contrato e segurança antes de encerrar a história

### Oportunidades de Paralelismo

- `T002` até `T005` podem rodar em paralelo durante o Setup
- `T007` até `T012`, `T014`, `T016` e `T017` podem rodar em paralelo durante o trabalho fundacional
- Tarefas de teste marcadas com `[P]` podem rodar em paralelo dentro de cada fase
- Tarefas de DTO, port, adapter e mapper marcadas com `[P]` podem rodar em paralelo dentro de uma história porque tocam arquivos separados
- Após a Fase 2, desenvolvedores diferentes podem se dividir entre US1, US2 e US4, mantendo a US3 atrás do comportamento compartilhado de sessão vindo da US2

---

## Exemplo de Paralelismo: User Story 1

```bash
Task: "Adicionar cobertura de integração do endpoint de cadastro em mod-auth/src/test/java/com/econference/auth/adapter/in/web/AuthRegistrationControllerIT.java"
Task: "Adicionar cobertura de integração dos endpoints de verificação e reenvio em mod-auth/src/test/java/com/econference/auth/adapter/in/web/AuthEmailVerificationControllerIT.java"
Task: "Adicionar validações do contrato auth para os fluxos de cadastro em mod-auth/src/test/java/com/econference/auth/contract/AuthRegistrationContractTest.java"

Task: "Criar RegisterRequest.java e RegisterResponse.java em mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/..."
Task: "Criar VerifyEmailRequest.java e ResendVerificationRequest.java em mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/request/"
Task: "Criar RegisterUserUseCase.java, VerifyEmailUseCase.java e ResendVerificationUseCase.java em mod-auth/src/main/java/com/econference/auth/application/port/in/"
```

## Exemplo de Paralelismo: User Story 2

```bash
Task: "Adicionar testes unitários do serviço JWT e do filtro de autenticação em mod-auth/src/test/java/com/econference/auth/security/JwtAuthenticationFilterTest.java"
Task: "Adicionar testes de persistência de sessão em mod-auth/src/test/java/com/econference/auth/adapter/out/persistence/UserSessionRepositoryAdapterIT.java"
Task: "Criar LoginRequest.java, LoginResponse.java e UserResponse.java em mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/..."
Task: "Criar LoginUseCase.java, LogoutUseCase.java e GetCurrentUserUseCase.java em mod-auth/src/main/java/com/econference/auth/application/port/in/"
```

## Exemplo de Paralelismo: User Story 4

```bash
Task: "Adicionar testes de integração de perfil de usuário e papéis em mod-auth/src/test/java/com/econference/auth/adapter/in/web/UserControllerIT.java"
Task: "Adicionar testes do contrato de identidade entre módulos em mod-auth/src/test/java/com/econference/auth/contract/UserIdentityPortContractTest.java"
Task: "Criar UpdateProfileRequest.java, PublicUserResponse.java e RolesResponse.java em mod-auth/src/main/java/com/econference/auth/adapter/in/web/dto/..."
Task: "Criar GetPublicUserUseCase.java, UpdateProfileUseCase.java e GetMyRolesUseCase.java em mod-auth/src/main/java/com/econference/auth/application/port/in/"
```

---

## Estratégia de Implementação

### MVP Primeiro

1. Concluir a Fase 1
2. Concluir a Fase 2
3. Concluir a Fase 3 (US1)
4. Validar o fluxo de cadastro e verificação de email de ponta a ponta
5. Demonstrar o MVP antes de expandir para os fluxos de sessão autenticada

### Entrega Incremental

1. Entregar a US1 para desbloquear cadastro e verificação de identidade
2. Entregar a US2 para desbloquear navegação autenticada do frontend e tratamento de sessão
3. Entregar a US3 para fechar as lacunas do ciclo de vida de senha
4. Entregar a US4 para suportar gerenciamento de perfil e consulta de identidade entre módulos
5. Concluir o hardening e a documentação da Fase 7 antes do release

### Escopo Sugerido de MVP

- **MVP principal**: Fase 1 + Fase 2 + Fase 3 (US1)
- **Próximo passo imediato**: Fase 4 (US2) para suportar a sessão autenticada do frontend

---

## Observações

- Toda tarefa usa o formato obrigatório de checklist: checkbox, task ID, `[P]` opcional, label de história obrigatório nas fases de user story e path exato de arquivo
- Os testes foram incluídos porque o plano explicita que cobertura de unidade, integração, contrato, persistência e arquitetura define o critério de pronto
- `specs/master` é agora o local canônico da especificação, plano, tarefas, contratos e checklists do auth