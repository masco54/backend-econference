# Research: Solicitação de Eventos

## Decision 1: Implementar a feature dentro de `mod-event` como uma nova fatia hexagonal

- **Decision**: Modelar `EventRequest` e seus casos de uso dentro de `mod-event`, com domínio, aplicação, adapters HTTP e adapters de persistência próprios.
- **Rationale**: O resultado final da aprovação é a criação e vinculação de um `Event`, que já pertence ao módulo. Isso mantém ownership de regra de negócio, reduz acoplamento e segue o padrão já consolidado em `mod-auth`.
- **Alternatives considered**: Criar um módulo separado só para solicitações foi rejeitado por introduzir uma fronteira artificial sem ganho claro; concentrar tudo em `application` foi rejeitado por violar a separação entre composition root e domínio.

## Decision 2: Preservar o contrato HTTP canônico de `event-requests.openapi.yaml`

- **Decision**: Planejar os endpoints públicos e administrativos conforme o artefato existente, com rotas `/public/event-requests`, `/public/event-requests/track`, `/public/event-requests/activate` e `/admin/event-requests...`.
- **Rationale**: O artefato OpenAPI já descreve a superfície que o consumidor espera. Reaproveitá-lo reduz ambiguidade e evita divergência entre especificação e implementação.
- **Alternatives considered**: Reencapsular tudo sob `/api/events/...` foi rejeitado porque quebraria o contrato já preparado; duplicar as duas superfícies foi rejeitado por ampliar manutenção e risco de inconsistência.

## Decision 3: Usar autenticação existente para administração e publicar um contrato explícito de ativação com `mod-auth`

- **Decision**: Admin endpoints usarão a infraestrutura JWT/cookie já existente e a ativação dependerá de um contrato publicado por `mod-auth` para localizar, criar ou vincular a conta do organizador.
- **Rationale**: `mod-auth` já é dono de contas, sessões, papéis e identidade. `mod-event` não deve acessar tabelas ou repositórios de autenticação diretamente.
- **Alternatives considered**: Acessar `users` diretamente a partir de `mod-event` foi rejeitado por violar a constituição; duplicar lógica de conta no módulo de eventos foi rejeitado por gerar inconsistência e duplicação de responsabilidades.

## Decision 4: Persistir solicitação e histórico em tabelas próprias do fluxo

- **Decision**: Criar `event_requests` para o agregado principal e `event_request_status_history` para a trilha auditável, com índices voltados a rastreio e listagem administrativa.
- **Rationale**: O fluxo exige histórico ordenado, notas administrativas, motivo de recusa, token de ativação e busca por status, tracking code e e-mail. Separar o histórico evita sobrecarga e simplifica consultas.
- **Alternatives considered**: Armazenar o histórico inteiro em uma coluna JSON foi rejeitado por dificultar busca, auditoria e portabilidade H2/PostgreSQL; manter somente o estado atual foi rejeitado por não atender rastreabilidade.

## Decision 5: Tratar token de ativação como credencial temporária de uso único

- **Decision**: Gerar token UUID aleatório, armazenar expiração e marcação de uso único no agregado da solicitação, e exigir validação antes de qualquer ativação.
- **Rationale**: O fluxo de aprovação precisa impedir reutilização, suportar reemissão controlada e permitir verificação pública antes da conclusão.
- **Alternatives considered**: Reutilizar o JWT de autenticação foi rejeitado porque o token serve outro contexto e outro dono; guardar token só em memória ou e-mail foi rejeitado por inviabilizar rastreamento e revogação.

## Decision 6: Reutilizar padrões existentes de erro, logging e health sem expandir `shared` desnecessariamente

- **Decision**: Manter health em `/api/events/health`, reutilizar o envelope de erro já tratado globalmente e introduzir exceções compartilhadas apenas se forem reaproveitáveis além do módulo.
- **Rationale**: O repositório já possui tratamento centralizado de exceções e logging estruturado no contorno HTTP. A nova feature precisa se encaixar nisso em vez de criar um segundo padrão.
- **Alternatives considered**: Criar um handler dedicado só para `mod-event` foi rejeitado por fragmentar a experiência; mover DTOs específicos para `shared` foi rejeitado por ser prematuro.

## Decision 7: Validar a feature com testes por camada e por fronteira

- **Decision**: Cobrir domínio e aplicação com unit tests, repositório com `@DataJpaTest`, endpoints com `MockMvc` e o contrato de ativação com testes de integração por porta publicada.
- **Rationale**: A feature combina validação de dados, regras de estado, integração HTTP, persistência e colaboração com autenticação. Um único tipo de teste não provaria todos esses riscos.
- **Alternatives considered**: Testes apenas end-to-end foram rejeitados por custo e baixa precisão diagnóstica; somente testes unitários foram rejeitados por não garantir wiring HTTP, persistência e segurança.

## Decision 8: Manter a migração incremental no conjunto atual de Flyway com dívida documentada sobre schema explícito

- **Decision**: Adicionar uma nova migração versionada em `application/src/main/resources/db/migration` para a persistência da feature, registrando a formalização completa por schema como dívida arquitetural controlada.
- **Rationale**: A convenção atual do repositório centraliza migrações no módulo `application`, com baseline legado em schema padrão. A feature precisa evoluir sem abrir uma refatoração transversal do banco.
- **Alternatives considered**: Pausar a feature até reestruturar todas as migrações históricas por schema foi rejeitado por expandir demais o escopo; criar banco físico separado por módulo foi rejeitado por violar a constituição.
