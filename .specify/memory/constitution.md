<!--
Sync Impact Report
- Version change: 1.1.0 -> 1.2.0
- Modified principles:
  - I. Code Quality and Architectural Boundaries are Release Gates -> I. Qualidade de Código e Fronteiras Arquiteturais são Gates de Release
  - II. Tests Define Done -> II. Testes Definem Pronto
  - III. Consistent User Experience Across Modules -> III. Experiência Consistente Entre Módulos
  - IV. Performance Budgets are Mandatory -> IV. Orçamentos de Performance são Obrigatórios
  - V. Hexagonal Modules and Operability -> V. Módulos Hexagonais, Operabilidade e Propriedade de Dados
- Added sections:
  - None
- Removed sections:
  - None
- Templates requiring updates:
  - ✅ updated `.specify/templates/plan-template.md`
  - ✅ updated `.specify/templates/spec-template.md`
  - ✅ updated `.specify/templates/tasks-template.md`
  - ⚠ not applicable `.specify/templates/commands/*.md` (directory not present in repository)
- Follow-up TODOs:
  - None
-->

# Constituição do Backend eConference

## Princípios Fundamentais

### I. Qualidade de Código e Fronteiras Arquiteturais são Gates de Release
Toda mudança em produção MUST preservar fronteiras claras entre `application`,
`shared` e os pacotes `mod-*`, e cada módulo MUST implementar explicitamente
Arquitetura Hexagonal e Clean Architecture. Modelo de domínio e serviços de domínio
MUST concentrar as regras de negócio. Serviços de aplicação MUST orquestrar casos de
uso e depender apenas de abstrações de domínio, ports e contratos estáveis entre
módulos. Código de framework, persistência, controladores HTTP, mensageria e demais
adaptadores MUST permanecer nas bordas e MUST NOT introduzir dependências para dentro
do domínio. O código MUST compilar sem avisos introduzidos pela mudança, usar nomes
claros, manter classes e métodos com responsabilidade única e evitar duplicação quando
o comportamento pertencer a uma abstração compartilhada. APIs públicas, DTOs, regras
de validação e contratos de erro MUST ser explícitos e revisados quanto à
retrocompatibilidade antes do merge.

Racional: este repositório é um monólito modular; regressões de qualidade ou vazamentos
arquiteturais em um módulo podem degradar silenciosamente múltiplos fluxos da
conferência se fronteiras, direção de dependências e manutenibilidade não forem
mantidas com rigor.

### II. Testes Definem Pronto
Toda alteração de comportamento MUST incluir testes automatizados no nível mais estreito
útil e em pelo menos um nível que comprove o impacto de integração quando contratos,
persistência, segurança ou wiring entre módulos mudarem. Correções de bug MUST adicionar
um teste de regressão que falha antes da correção. A mudança não está pronta até que as
suítes Maven relevantes passem localmente ou na CI. Testes para mudanças arquiteturais
MUST provar a fronteira pretendida, como comportamento de caso de uso via ports,
comportamento de adaptadores nas bordas de integração ou restrições de dependência entre
módulos quando o risco não for trivial. Pular testes exige justificativa escrita no
plano e no registro de revisão.

Racional: operações de conferência dependem de fluxos previsíveis de ponta a ponta, como
autenticação, eventos, submissões, revisões e inscrições; testes são a única evidência
durável de que esses fluxos e suas fronteiras continuam íntegros.

### III. Experiência Consistente Entre Módulos
Todo comportamento externamente visível MUST parecer consistente entre módulos:
padronização de endpoints, semântica HTTP, feedback de validação, convenções de
paginação e filtros, respostas de segurança e health checks MUST seguir os mesmos
padrões, salvo exceção documentada e aprovada. Mudanças de API MUST atualizar exemplos
ou documentação quando alterarem comportamento visível ao consumidor. Mensagens de erro
retornadas aos clientes MUST ser acionáveis, estáveis e livres de detalhes internos de
implementação.

Racional: o backend sustenta uma única experiência de produto, embora a funcionalidade
esteja dividida em módulos; inconsistência aumenta custo de suporte e risco de
integração.

### IV. Orçamentos de Performance são Obrigatórios
Funcionalidades novas ou alteradas MUST declarar o impacto esperado em latência,
consultas e consumo de recursos durante o planejamento. Handlers de requisição no
caminho crítico MUST mirar p95 abaixo de 300 ms para leituras típicas de um módulo e p95
abaixo de 500 ms para escritas ou orquestrações entre módulos em condições locais de
referência, salvo orçamento diferente aprovado na especificação. Mudanças MUST evitar
padrões óbvios de N+1, serialização desnecessária e processamento em memória sem
limites. Caminhos sensíveis a performance MUST incluir medição, testes focados ou
benchmark quando o risco não for trivial.

Racional: prazos de submissão e períodos de revisão geram picos de tráfego; orçamentos
de performance evitam que lentidões evitáveis se acumulem entre módulos.

### V. Módulos Hexagonais, Operabilidade e Propriedade de Dados
Cada módulo MUST modelar seu comportamento com camadas internas de aplicação e domínio,
ports explícitos para dependências de saída e adaptadores para infraestrutura e entrega.
A direção de dependências MUST sempre apontar para dentro: adaptadores podem depender de
contratos de aplicação/domínio, mas o domínio MUST NOT depender de Spring MVC, JPA,
clientes de mensageria ou outras APIs específicas de framework, salvo se a dependência
estiver isolada atrás de uma abstração estável possuída pelo módulo e explicitamente
justificada. Colaboração entre módulos MUST ocorrer por interfaces explícitas,
contratos publicados ou serviços de aplicação, e nunca por acesso oculto aos internos de
outro módulo. A configuração MUST permanecer sensível ao ambiente e segura por padrão.
Toda funcionalidade exposta por módulo MUST preservar ou ampliar health visibility,
logging estruturado e modos de falha que facilitem diagnóstico sem expor segredos.

A arquitetura de dados deste sistema MUST usar um único banco de dados físico. Os módulos
MUST NOT possuir bancos físicos separados. O isolamento de dados entre módulos MUST ser
feito por schema, e cada módulo MUST ser dono de seus limites de schema, tabelas,
migrações, repositórios e contratos de persistência mesmo compartilhando a mesma
instância de banco. Um módulo MUST NOT ler ou escrever diretamente nas tabelas de outro
schema; quando precisar de dados ou comportamento de outro módulo, MUST usar contratos,
ports ou serviços de aplicação explicitamente publicados pelo módulo proprietário.
Mudanças de persistência MUST documentar impacto em schema, migração, rollback,
compatibilidade local com H2/PostgreSQL e qualquer efeito de performance.

Racional: o sistema é um monólito modular com deployment compartilhado, mas a propriedade
de dados e as fronteiras entre módulos ainda precisam ser explícitas; banco único reduz
complexidade operacional, enquanto isolamento por schema preserva autonomia de módulo,
clareza de contratos e segurança contra acoplamento indevido.

## Restrições de Engenharia

- A stack canônica deste repositório é Java 24, Spring Boot 3.4, Maven, H2 para
  execuções locais em memória e PostgreSQL para o perfil persistente.
- Toda mudança em módulo MUST identificar sua camada de aplicação, camada de domínio,
  adaptadores de entrada e adaptadores de saída. Se algum deles não se aplicar, o plano
  MUST declarar o motivo.
- Ports MUST ser possuídos pelo módulo que define o caso de uso ou a necessidade de
  domínio. Adaptadores MUST implementar esses ports na borda de infraestrutura e MUST
  permanecer substituíveis.
- Código compartilhado pertence a `shared` somente quando pelo menos dois módulos
  dependem da mesma abstração estável; caso contrário, MUST permanecer no módulo dono.
- `application` MUST permanecer como composition root e ponto de montagem do wiring entre
  módulos; regras de negócio específicas de funcionalidade MUST NOT migrar para lá.
- Todas as APIs de módulo MUST expor ou preservar health checks consistentes com o padrão
  existente `/api/.../health` quando o módulo servir tráfego HTTP.
- Persistência MUST usar um único banco físico com separação por schema por módulo.
  Qualquer proposta de banco físico por módulo viola esta constituição, salvo emenda
  formal da própria constituição.
- Cada módulo MUST ser dono de seu schema e de seus contratos de persistência, mesmo no
  banco compartilhado. Dependências entre schemas MUST ser tratadas como integrações
  explícitas e nunca como atalho para burlar limites de módulo.
- Mudanças em banco, mensageria ou cache MUST documentar migração, rollback e impacto no
  desenvolvimento local.

## Fluxo de Entrega e Gates de Qualidade

- Toda especificação de funcionalidade MUST declarar expectativas de qualidade de código,
  cobertura de testes, restrições de consistência para o usuário, metas de performance,
  estratégia de persistência e a divisão pretendida entre domínio / aplicação /
  adaptadores antes da implementação.
- Todo plano de implementação MUST passar por um Constitution Check que confirme impacto
  nas fronteiras dos módulos, desenho de ports e adapters, direção de dependências,
  estratégia de testes, consistência UX/API, orçamento de performance e aderência à
  regra de banco único com isolamento por schema.
- Quebras de tarefa MUST incluir trabalho para ports, adapters, serviços de aplicação,
  lógica de domínio, testes, atualizações de consistência/documentação, validação de
  performance e verificação de schema/contratos de persistência sempre que esses pontos
  forem afetados.
- Antes do merge, o responsável pela mudança MUST verificar build bem-sucedido, testes
  automatizados direcionados e smoke checks relevantes para os módulos afetados.
- Se um princípio não puder ser atendido, a exceção MUST ser documentada no plano com
  escopo, racional, mitigação e prazo de expiração ou tarefa de acompanhamento.

## Governança

Esta constituição prevalece sobre práticas locais conflitantes deste repositório.
Emendas MUST ser registradas em `.specify/memory/constitution.md`, incluir um Sync Impact
Report e atualizar, na mesma mudança, os templates dependentes quando as expectativas de
workflow forem alteradas.

A política de versionamento segue versionamento semântico para documentos de governança:
- MAJOR: remove ou redefine materialmente um princípio ou requisito de governança.
- MINOR: adiciona um novo princípio/seção ou expande materialmente uma prática
  obrigatória.
- PATCH: esclarece redação, corrige erros de digitação ou faz refinamentos semânticos não
  materiais.

Revisão de conformidade é obrigatória para todo plano e pull request que afete código de
produção. As revisões MUST verificar conformidade com os cinco princípios fundamentais,
incluindo validação explícita das fronteiras hexagonais, da direção de dependências e da
regra de persistência em banco único com ownership por schema. Violações não resolvidas
MUST ser aprovadas explicitamente como exceções temporárias e com prazo definido. A data
de ratificação registra a primeira adoção formal desta constituição; a data da última
emenda MUST ser atualizada sempre que houver mudanças substantivas.

**Version**: 1.2.0 | **Ratified**: 2026-03-29 | **Last Amended**: 2026-03-29
