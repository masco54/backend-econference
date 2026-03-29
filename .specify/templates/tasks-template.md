---

description: "Template de lista de tarefas para implementação de funcionalidades"
---

# Tasks: [FEATURE NAME]

**Input**: Documentos de design em `/specs/[###-feature-name]/`
**Prerequisites**: plan.md (obrigatório), spec.md (obrigatório para user stories), research.md, data-model.md, contracts/

**Tests**: Inclua tarefas de teste sempre que comportamento, contratos, persistência, segurança, fronteiras arquiteturais ou correções de bug mudarem. Se nenhum teste automatizado for adicionado, as tarefas geradas MUST nomear a justificativa aprovada.

**Organization**: As tarefas são agrupadas por user story para permitir implementação e teste independentes de cada história.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Pode executar em paralelo (arquivos diferentes, sem dependências)
- **[Story]**: A qual user story a tarefa pertence (ex.: `US1`, `US2`, `US3`)
- Inclua caminhos de arquivo exatos nas descrições

## Path Conventions

- **Backend Maven multi-módulo (este repositório)**: `application/src/`, `shared/src/`, `mod-auth/src/`, `mod-event/src/`, `mod-submission/src/`, `mod-review/src/`, `mod-committee/src/`, `mod-registration/src/`
- **Camadas hexagonais dentro de um módulo**: tarefas geradas MUST usar os caminhos reais de pacote/arquivo para modelo/serviços de domínio, serviços/casos de uso de aplicação, adaptadores de entrada (por exemplo controllers), ports de saída e adaptadores de saída (por exemplo implementações de persistência ou integração)
- **Persistência**: tarefas geradas MUST indicar o schema do módulo dono, scripts de migração correspondentes e qualquer validação necessária para confirmar que não há acesso direto ao schema de outro módulo nem criação de banco físico por módulo
- **Tests**: use as raízes de teste correspondentes do módulo (por exemplo `mod-event/src/test/`) e organize por intenção de unidade, integração, contrato ou fronteira arquitetural conforme definido no plano
- Os caminhos abaixo são apenas exemplos; as tarefas geradas MUST usar os módulos e caminhos reais definidos em `plan.md`

<!-- 
  ============================================================================
  IMPORTANTE: As tarefas abaixo são EXEMPLOS apenas para ilustração.

  O comando /speckit.tasks MUST substituí-las por tarefas reais com base em:
  - User stories de spec.md (com prioridades P1, P2, P3...)
  - Requisitos de funcionalidade de plan.md
  - Entidades de data-model.md
  - Endpoints de contracts/

  As tarefas MUST ser organizadas por user story para que cada história possa ser:
  - Implementada independentemente
  - Testada independentemente
  - Entregue como incremento de MVP

  NÃO mantenha estas tarefas de exemplo no arquivo gerado tasks.md.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Inicialização do trabalho e estrutura básica

- [ ] T001 Criar a estrutura do projeto conforme o plano de implementação
- [ ] T002 Inicializar dependências e configuração da funcionalidade no(s) módulo(s) correto(s)
- [ ] T003 [P] Configurar ou ajustar linting, formatação e análise estática quando aplicável
- [ ] T004 [P] Registrar no `plan.md` o orçamento de performance, necessidades de observabilidade, fronteiras arquiteturais e abordagem de validação
- [ ] T005 [P] Registrar no `plan.md` o schema dono da funcionalidade, as migrações necessárias e a confirmação de aderência ao banco físico único

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Infraestrutura central que MUST estar pronta antes de QUALQUER user story

**⚠️ CRITICAL**: Nenhum trabalho de user story pode começar até esta fase estar concluída

Exemplos de tarefas fundacionais (ajuste conforme o projeto):

- [ ] T006 Configurar schema, migrations e convenções de persistência do módulo dono
- [ ] T007 [P] Implementar autenticação/autorização necessária
- [ ] T008 [P] Configurar roteamento/API e estrutura base de adaptadores
- [ ] T009 Criar modelos base/entidades e ports possuídos pelo módulo de que todas as histórias dependem
- [ ] T010 Configurar tratamento de erros, logging estruturado e health visibility
- [ ] T011 Configurar gerenciamento de ambiente e propriedades
- [ ] T012 [P] Definir ou validar convenções compartilhadas de resposta/erro para consistência entre módulos
- [ ] T013 [P] Definir ou validar guardrails de direção de dependência para os módulos/pacotes afetados quando o risco justificar
- [ ] T014 Validar que integrações com outros módulos ocorrerão por contratos explícitos e não por leitura/escrita direta em schema alheio

**Checkpoint**: Fundação pronta - a implementação das user stories pode agora começar em paralelo

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Breve descrição do que esta história entrega]

**Independent Test**: [Como verificar que esta história funciona sozinha]

### Tests for User Story 1 ⚠️

> **NOTE: Escreva estes testes PRIMEIRO e garanta que FALHEM antes da implementação**

- [ ] T015 [P] `US1` Teste de contrato ou consistência de API para [endpoint/comportamento do módulo] em [caminho real de teste]
- [ ] T016 [P] `US1` Teste de integração ou regressão para [jornada do usuário] em [caminho real de teste]
- [ ] T017 [P] `US1` Teste de fronteira arquitetural ou direção de dependência para [regra de módulo/pacote] em [caminho real de teste]
- [ ] T018 [P] `US1` Teste focado de persistência para provar ownership do schema e ausência de acesso direto a schema de outro módulo em [caminho real de teste]
- [ ] T019 [P] `US1` Verificação focada de performance ou benchmark para [fluxo crítico] quando o risco justificar

### Implementation for User Story 1

- [ ] T020 [P] `US1` Criar ou atualizar modelo de domínio em [caminho real de código]
- [ ] T021 [P] `US1` Definir ou atualizar ports de entrada/saída em [caminho real de código]
- [ ] T022 `US1` Implementar serviço de aplicação/caso de uso em [caminho real de código]
- [ ] T023 `US1` Implementar adaptador de entrada ou saída em [caminho real de código]
- [ ] T024 `US1` Aplicar mudanças de schema/migração e contratos de persistência no módulo dono em [caminho real]
- [ ] T025 `US1` Adicionar validação, tratamento de erros e ajustes de consistência de resposta visível ao usuário
- [ ] T026 `US1` Adicionar logging/health/metrics necessários para operabilidade

**Checkpoint**: Neste ponto, a User Story 1 deve estar totalmente funcional e testável de forma independente

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Breve descrição do que esta história entrega]

**Independent Test**: [Como verificar que esta história funciona sozinha]

### Tests for User Story 2 ⚠️

- [ ] T027 [P] `US2` Teste de contrato ou consistência de API para [endpoint/comportamento do módulo] em [caminho real de teste]
- [ ] T028 [P] `US2` Teste de integração ou regressão para [jornada do usuário] em [caminho real de teste]
- [ ] T029 [P] `US2` Teste de fronteira arquitetural ou direção de dependência para [regra de módulo/pacote] em [caminho real de teste]
- [ ] T030 [P] `US2` Teste de persistência/integração provando uso de contrato explícito entre módulos sem acesso cruzado de schema em [caminho real de teste]
- [ ] T031 [P] `US2` Verificação focada de performance ou benchmark para [fluxo crítico] quando o risco justificar

### Implementation for User Story 2

- [ ] T032 [P] `US2` Criar ou atualizar modelo de domínio em [caminho real de código]
- [ ] T033 [P] `US2` Definir ou atualizar ports de entrada/saída em [caminho real de código]
- [ ] T034 `US2` Implementar serviço de aplicação/caso de uso em [caminho real de código]
- [ ] T035 `US2` Implementar adaptador de entrada ou saída em [caminho real de código]
- [ ] T036 `US2` Integrar com módulos existentes somente por contratos explícitos publicados pelo módulo dono
- [ ] T037 `US2` Atualizar documentação/exemplos se o comportamento visível ao usuário mudar

**Checkpoint**: Neste ponto, as User Stories 1 E 2 devem funcionar de forma independente

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Breve descrição do que esta história entrega]

**Independent Test**: [Como verificar que esta história funciona sozinha]

### Tests for User Story 3 ⚠️

- [ ] T038 [P] `US3` Teste de contrato ou consistência de API para [endpoint/comportamento do módulo] em [caminho real de teste]
- [ ] T039 [P] `US3` Teste de integração ou regressão para [jornada do usuário] em [caminho real de teste]
- [ ] T040 [P] `US3` Teste de fronteira arquitetural ou direção de dependência para [regra de módulo/pacote] em [caminho real de teste]
- [ ] T041 [P] `US3` Teste de persistência ou migração para [schema/contrato] em [caminho real de teste]
- [ ] T042 [P] `US3` Verificação focada de performance ou benchmark para [fluxo crítico] quando o risco justificar

### Implementation for User Story 3

- [ ] T043 [P] `US3` Criar ou atualizar modelo de domínio em [caminho real de código]
- [ ] T044 [P] `US3` Definir ou atualizar ports de entrada/saída em [caminho real de código]
- [ ] T045 `US3` Implementar serviço de aplicação/caso de uso em [caminho real de código]
- [ ] T046 `US3` Implementar adaptador de entrada ou saída em [caminho real de código]
- [ ] T047 `US3` Verificar consistência de semântica de validação/erro com os demais módulos

**Checkpoint**: Todas as user stories devem agora estar funcionais de forma independente

---

[Adicione mais fases de user story conforme necessário, seguindo o mesmo padrão]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Melhorias que afetam múltiplas user stories

- [ ] TXXX [P] Atualizações de documentação e README/quickstart para qualquer comportamento de API alterado
- [ ] TXXX Limpeza e refatoração de código
- [ ] TXXX Verificar wiring de ports/adapters, direção de dependências, ownership de schema e documentação de fronteiras entre todos os módulos/histórias afetados
- [ ] TXXX Validar migrations, rollback e compatibilidade H2/PostgreSQL para os schemas afetados
- [ ] TXXX Verificação de performance em todas as histórias/módulos afetados
- [ ] TXXX [P] Testes adicionais de unidade/integração/regressão/arquitetura necessários para cobertura de risco
- [ ] TXXX Hardening de segurança
- [ ] TXXX Executar validação de `quickstart.md` e os comandos Maven de teste/build afetados

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Sem dependências - pode começar imediatamente
- **Foundational (Phase 2)**: Depende da conclusão do Setup - BLOQUEIA todas as user stories
- **User Stories (Phase 3+)**: Todas dependem da conclusão da fase Foundational
  - As user stories podem então prosseguir em paralelo (se houver equipe)
  - Ou sequencialmente por ordem de prioridade (P1 → P2 → P3)
- **Polish (Final Phase)**: Depende da conclusão de todas as user stories desejadas

### User Story Dependencies

- **User Story 1 (P1)**: Pode começar após Foundational (Phase 2) - Sem dependências de outras histórias
- **User Story 2 (P2)**: Pode começar após Foundational (Phase 2) - Pode integrar com US1, mas deve ser testável de forma independente
- **User Story 3 (P3)**: Pode começar após Foundational (Phase 2) - Pode integrar com US1/US2, mas deve ser testável de forma independente

### Within Each User Story

- Testes MUST ser escritos e FALHAR antes da implementação quando cobrirem novo comportamento ou correção de bug
- Modelos e regras de domínio antes de serviços de aplicação
- Ports antes dos adaptadores que os implementam ou consomem
- Serviços de aplicação antes de endpoints/adaptadores
- Implementação central antes da integração
- Verificações de performance, UX e persistência por schema antes de encerrar a história
- A história deve estar completa antes de mover para a próxima prioridade

### Parallel Opportunities

- Todas as tarefas de Setup marcadas com [P] podem executar em paralelo
- Todas as tarefas Foundational marcadas com [P] podem executar em paralelo (dentro da Phase 2)
- Após a conclusão da fase Foundational, todas as user stories podem começar em paralelo (se a capacidade do time permitir)
- Todos os testes de uma user story marcados com [P] podem executar em paralelo
- Modelos dentro de uma história marcados com [P] podem executar em paralelo
- User stories diferentes podem ser trabalhadas em paralelo por membros diferentes da equipe

---

## Parallel Example: User Story 1

```bash
# Executar os testes da User Story 1 em paralelo (se solicitados):
Task: "Teste de contrato para [endpoint] em [caminho real de teste]"
Task: "Teste de integração para [jornada do usuário] em [caminho real de teste]"
Task: "Teste de ownership do schema em [caminho real de teste]"

# Executar tarefas de modelagem da User Story 1 em paralelo:
Task: "Criar [Entidade1] em [caminho real de código]"
Task: "Criar [Entidade2] em [caminho real de código]"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Concluir a Phase 1: Setup
2. Concluir a Phase 2: Foundational (CRITICAL - bloqueia todas as histórias)
3. Concluir a Phase 3: User Story 1
4. **STOP and VALIDATE**: Testar a User Story 1 de forma independente
5. Fazer deploy/demo se estiver pronto

### Incremental Delivery

1. Concluir Setup + Foundational → base pronta
2. Adicionar User Story 1 → testar independentemente → deploy/demo (MVP)
3. Adicionar User Story 2 → testar independentemente → deploy/demo
4. Adicionar User Story 3 → testar independentemente → deploy/demo
5. Cada história agrega valor sem quebrar as anteriores

### Parallel Team Strategy

Com múltiplos desenvolvedores:

1. O time conclui Setup + Foundational junto
2. Quando Foundational terminar:
   - Desenvolvedor A: User Story 1
   - Desenvolvedor B: User Story 2
   - Desenvolvedor C: User Story 3
3. As histórias são concluídas e integradas de forma independente

---

## Notes

- [P] tasks = arquivos diferentes, sem dependências
- [Story] label mapeia a tarefa para uma user story específica para rastreabilidade
- Cada user story deve ser completável e testável de forma independente
- Verifique se os testes falham antes de implementar
- Inclua tarefas explícitas para consistência de resposta, atualizações de documentação, validação de performance e verificação de fronteira arquitetural quando afetadas
- Faça commit após cada tarefa ou grupo lógico
- Pare em qualquer checkpoint para validar a história de forma independente
- Evite: tarefas vagas, conflitos de mesmo arquivo, acoplamento cruzado de módulos oculto, vazamentos de direção de dependência e pular testes sem justificativa
