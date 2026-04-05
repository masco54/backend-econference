# Plano de Implementação: Solicitação de Eventos

**Branch**: `001-event-request-endpoints` | **Date**: 2026-04-04 | **Spec**: `C:\Users\marco\projetos econference\back-econference\specs\001-event-request-endpoints\spec.md`
**Input**: Especificação da funcionalidade em `C:\Users\marco\projetos econference\back-econference\specs\001-event-request-endpoints\spec.md`

## Summary

Implementar o fluxo de solicitação pública de eventos no `mod-event` com uma fatia hexagonal completa para submissão pública, acompanhamento, revisão administrativa e ativação pós-aprovação. O desenho reutiliza o padrão de ports e adapters já consolidado em `mod-auth`, mantém a criação do evento aprovado dentro do módulo de eventos, integra autenticação/autorização com a infraestrutura JWT já existente e adiciona contratos explícitos com `mod-auth` para provisionamento ou vínculo do organizador sem acesso direto às tabelas de autenticação.

## Technical Context

**Language/Version**: Java 24  
**Primary Dependencies**: Spring Boot 3.4.4, Spring Web, Spring Data JPA/Hibernate, Flyway, Jakarta Bean Validation, Lombok, Spring Security/JWT já existente em `mod-auth`  
**Storage**: H2 em memória para desenvolvimento/testes e PostgreSQL no profile persistente; banco físico único com migrações centralizadas em `application/src/main/resources/db/migration` e ownership funcional do dado em `mod-event`  
**Testing**: JUnit 5, Spring Boot Test, MockMvc, `@DataJpaTest`, Spring Security Test, testes de regressão de contratos HTTP e integração entre `mod-event` e `mod-auth` via contracts/ports  
**Target Platform**: aplicação Spring Boot executando em JVM, localmente em Windows/Linux e em container no ambiente persistente  
**Project Type**: monólito modular backend / web-service  
**Performance Goals**: p95 < 500 ms para submissão, aprovação, rejeição e ativação; p95 < 300 ms para acompanhamento e listagem administrativa paginada; nenhuma consulta N+1 na listagem administrativa e no detalhamento com histórico  
**Constraints**: preservar fronteiras hexagonais; manter regra de dependência apontando para dentro; não acessar tabelas de `mod-auth` diretamente; seguir o contrato HTTP de `event-requests.openapi.yaml`; registrar transições e auditoria de estado; manter compatibilidade H2/PostgreSQL; evitar acoplamento de framework no domínio  
**Scale/Scope**: módulos primariamente afetados `mod-event`, `application`, `shared` e `mod-auth`; fluxo com volume administrativo moderado e acesso público bursty; paginação alvo entre 10 e 100 itens por página; histórico curto por solicitação  
**Architecture Boundaries**: `mod-event` receberá novas camadas de domínio, aplicação, adapters HTTP e persistência para `EventRequest`; `mod-auth` continuará dono da autenticação e deverá publicar um contrato explícito de provisionamento/vínculo de conta para a ativação; `shared` absorverá apenas contratos ou exceções comprovadamente compartilhadas; `application` continuará como composition root e dono das migrações Flyway  
**Data Architecture**: a funcionalidade adicionará tabelas `event_requests` e `event_request_status_history` como persistência possuída por `mod-event`, com migração versionada em `application`; o vínculo com `events` continuará por `linked_event_id`; nenhum acesso direto ao schema/tabelas de `mod-auth` será permitido, e a ativação dependerá de contrato publicado por esse módulo

## Constitution Check

*GATE: MUST passar antes da Fase 0 de pesquisa. Revalidar após a Fase 1 de design.*

### Pre-Design Gate

- **Qualidade de Código**: PASS. A mudança permanece centrada em `mod-event`; `shared` só receberá abstrações comprovadamente compartilhadas; `application` fica restrito a migrações e wiring.
- **Arquitetura Hexagonal/Clean**: PASS. O domínio novo ficará em `mod-event/domain`; casos de uso e ports em `mod-event/application`; controllers HTTP em `mod-event/adapter/in/web`; persistência e notificações em `mod-event/adapter/out`; integração com autenticação ocorrerá por contrato publicado de `mod-auth`.
- **Testes**: PASS. O plano inclui testes unitários para regras de estado, `@DataJpaTest` para busca/paginação/duplicidade, `MockMvc` para superfície pública e administrativa e regressões para ativação e transições inválidas.
- **Consistência de UX/API**: PASS COM EXCEÇÃO DOCUMENTADA. O contrato externo canônico já existe em `event-requests.openapi.yaml` com rotas `/public/...` e `/admin/...`; a feature seguirá esse contrato e manterá consistência de semântica HTTP, erros e paginação mesmo divergindo do prefixo `/api/events` usado no health.
- **Performance**: PASS. O desenho prevê índices por status/data e por código de rastreio/e-mail, paginação obrigatória para admin e criação de histórico sem consultas desnecessárias.
- **Arquitetura de Dados**: PASS COM RISCO CONTROLADO. O repositório ainda carrega migrações legadas em schema padrão; a feature manterá banco físico único e ownership funcional em `mod-event`, sem tocar dados de `mod-auth`, e registrará a dívida de formalizar schema explícito do módulo sem bloquear a entrega.
- **Exceptions**: Exceção 1: rotas públicas/administrativas fora do prefixo `/api/events` por causa do contrato já definido. Exceção 2: persistência nova com ownership de módulo no conjunto atual de migrações legadas antes da consolidação completa por schema.

### Post-Design Recheck

- **Qualidade de Código**: PASS. `mod-event` permanece dono do fluxo; nenhuma regra de negócio é deslocada para `application`.
- **Arquitetura Hexagonal/Clean**: PASS. O design final inclui ports explícitos para persistência, notificação e provisionamento de conta, sem leitura direta de repositórios de outro módulo.
- **Testes**: PASS. Os artefatos de design e quickstart definem suítes mínimas para domínio, integração de persistência, HTTP e integração de contrato.
- **Consistência de UX/API**: PASS COM EXCEÇÃO DOCUMENTADA. O contrato HTTP foi mantido conforme artefato canônico e padronizado em códigos de status, paginação e erros.
- **Performance**: PASS. O modelo prevê índices, paginação e busca controlada; o quickstart define verificação prática dos caminhos críticos.
- **Arquitetura de Dados**: PASS COM EXCEÇÃO DOCUMENTADA. O design não introduz banco físico por módulo nem acesso cruzado direto; a dívida remanescente é a formalização total de schema do módulo legado.
- **Exceptions**: Mantidas e rastreadas em `Complexity Tracking`.

## Project Structure

### Documentation (this feature)

```text
specs/001-event-request-endpoints/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── event-requests-http.md
│   └── account-provisioning-port.md
└── tasks.md
```

### Source Code (repository root)

```text
application/
└── src/main/resources/db/migration/

shared/
└── src/main/java/com/econference/shared/
    ├── contract/
    └── exception/

mod-auth/
└── src/main/java/com/econference/auth/
    ├── adapter/out/identity/
    ├── application/port/
    ├── application/service/
    ├── controller/
    └── security/

mod-event/
├── src/main/java/com/econference/event/
│   ├── adapter/
│   │   ├── in/web/
│   │   └── out/
│   ├── application/
│   │   ├── port/in/
│   │   ├── port/out/
│   │   └── service/
│   ├── controller/
│   ├── domain/
│   ├── repository/
│   └── service/
└── src/test/java/com/econference/event/
```

**Structure Decision**: A funcionalidade pertence a `mod-event` porque o agregado final criado é `Event` e o ciclo de aprovação é parte do domínio de entrada e governança de eventos. O módulo receberá uma nova fatia hexagonal para `EventRequest`, mantendo controllers finos, casos de uso explícitos, persistência atrás de ports e criação do `Event` aprovado dentro do próprio módulo. `mod-auth` não será acessado por tabela ou repositório; ele publicará contrato de leitura/provisionamento consumido por `mod-event`. `application` continuará como ponto único de migração e composição. `shared` só absorverá contratos ou exceções quando houver reuso real entre módulos.

## Phase 0 Research Summary

- Reutilizar o padrão consolidado em `mod-auth`: serviço de aplicação implementando múltiplos `*UseCase`, DTOs na borda HTTP, adapters de persistência separados e autenticação no contorno.
- Manter o contrato HTTP canônico já existente em `event-requests.openapi.yaml` em vez de reinventar rotas.
- Tratar a ativação como colaboração entre módulos por contrato explícito: `mod-event` continua dono da solicitação e do evento; `mod-auth` continua dono da conta do usuário e dos papéis.
- Persistir histórico de status como coleção própria para permitir trilha auditável sem sobrecarregar o agregado principal.
- Usar migração Flyway incremental em `application` para novas tabelas, índices e restrições de duplicidade.

Consulte `research.md`, `data-model.md`, `contracts/` e `quickstart.md` para o detalhamento de design.

## Phase 1 Design Outputs

- `research.md`: decisões técnicas, trade-offs e alternativas rejeitadas.
- `data-model.md`: entidades, estados, relações, invariantes e estratégia de persistência.
- `contracts/event-requests-http.md`: contrato HTTP canônico da funcionalidade.
- `contracts/account-provisioning-port.md`: contrato interno entre `mod-event` e `mod-auth` para ativação.
- `quickstart.md`: fluxo de validação local e roteiro de teste manual/automatizado.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| Rotas públicas/administrativas fora de `/api/events` | O contrato externo de referência já existe e precisa ser preservado para não invalidar consumidores e documentação | Reenquadrar tudo em `/api/events/...` quebraria o artefato HTTP fornecido e aumentaria retrabalho sem ganho funcional imediato |
| Ownership lógico do módulo antes da formalização completa por schema explícito | As migrações legadas V1/V2 ainda estão no schema padrão e a feature precisa evoluir sem uma migração estrutural ampla do banco inteiro | Parar a feature para refatorar toda a estratégia histórica de schema atrasaria a entrega e expandiria escopo muito além do fluxo de solicitação de eventos |

