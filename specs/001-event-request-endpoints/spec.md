# Especificação de Funcionalidade: Solicitação de Eventos

**Feature Branch**: `001-event-request-endpoints`  
**Created**: 2026-04-04  
**Status**: Draft  
**Input**: Descrição do usuário: "preciso criar o endpoint no modulo de events. E a solicitação de eventos. use para especificar data-model-event-request.md e event-equest.openapi.yaml"

## User Scenarios & Testing *(obrigatório)*

### User Story 1 - Enviar solicitação pública de evento (Priority: P1)

Como visitante não autenticado, quero solicitar a criação de um evento informando meus dados de contato e os dados básicos do evento para iniciar o processo sem depender de um cadastro prévio.

**Why this priority**: Sem a submissão pública, o fluxo de aquisição de novos eventos não existe. Esta é a menor fatia que entrega valor para organizadores em potencial e para a operação do produto.

**Independent Test**: Enviar uma solicitação pública com dados válidos para a fronteira HTTP do módulo de eventos e confirmar que a resposta cria a solicitação, devolve um identificador de acompanhamento e deixa a requisição disponível para consulta administrativa posterior.

**Acceptance Scenarios**:

1. **Given** um visitante com nome, e-mail e dados do evento válidos, **When** ele envia a solicitação pública, **Then** o sistema registra a solicitação com status inicial de recebida e retorna um código de acompanhamento único.
2. **Given** um visitante que já possui uma solicitação pendente para o mesmo evento com o mesmo e-mail, **When** ele tenta enviar outra solicitação equivalente, **Then** o sistema rejeita a duplicidade com mensagem de conflito compreensível.
3. **Given** um visitante com datas inconsistentes ou campos obrigatórios ausentes, **When** ele envia a solicitação, **Then** o sistema rejeita a operação sem criar nenhum registro parcial.

---

### User Story 2 - Acompanhar e ativar uma solicitação aprovada (Priority: P2)

Como solicitante, quero acompanhar o andamento da minha solicitação e, quando ela for aprovada, concluir a ativação para assumir o evento como organizador principal.

**Why this priority**: Depois da submissão, o solicitante precisa visibilidade sobre o andamento e um caminho claro para transformar aprovação em evento utilizável. Sem isso, a jornada fica interrompida no meio.

**Independent Test**: Consultar uma solicitação existente usando o código de acompanhamento e o e-mail do solicitante; depois validar e consumir um token de ativação de uma solicitação aprovada, confirmando que o evento fica vinculado ao solicitante correto.

**Acceptance Scenarios**:

1. **Given** um solicitante com código de acompanhamento e e-mail corretos, **When** ele consulta sua solicitação, **Then** o sistema retorna o status atual, o histórico de mudanças e os dados essenciais do evento solicitado.
2. **Given** uma combinação inválida de código e e-mail, **When** o solicitante tenta consultar a solicitação, **Then** o sistema retorna uma resposta genérica de não encontrado sem expor se o código ou o e-mail estão corretos isoladamente.
3. **Given** uma solicitação aprovada com token de ativação válido, **When** o solicitante conclui a ativação, **Then** o sistema vincula a solicitação a um usuário elegível e ao evento aprovado, impedindo reutilização do mesmo token.

---

### User Story 3 - Revisar solicitações como administrador (Priority: P3)

Como administrador, quero listar, consultar, aprovar, rejeitar e anotar solicitações de evento para controlar a entrada de novos eventos na plataforma com rastreabilidade operacional.

**Why this priority**: A submissão pública só gera valor real para o negócio se houver um fluxo administrativo que permita triagem, decisão e comunicação do resultado.

**Independent Test**: Exercitar a superfície administrativa do módulo de eventos para listar solicitações por status, abrir detalhes, registrar nota, aprovar uma solicitação elegível, rejeitar outra com motivo obrigatório e reemitir um token expirado quando aplicável.

**Acceptance Scenarios**:

1. **Given** um administrador autenticado, **When** ele lista solicitações com filtros de status e busca textual, **Then** o sistema retorna uma lista paginada coerente com os critérios informados.
2. **Given** uma solicitação em análise ou recém-recebida, **When** o administrador adiciona uma nota, **Then** o histórico registra a interação e a solicitação permanece rastreável com o contexto administrativo.
3. **Given** uma solicitação apta para decisão, **When** o administrador aprova ou rejeita a solicitação, **Then** o sistema registra a decisão, exige motivo na rejeição e bloqueia transições incompatíveis com o estado atual.

### Edge Cases

- O sistema deve rejeitar solicitações cuja data final seja anterior à data inicial.
- O sistema deve impedir múltiplas solicitações pendentes com a mesma combinação de e-mail do solicitante e nome do evento.
- O acompanhamento público deve responder de forma indistinguível para combinação inválida de código e e-mail, reduzindo enumeração de dados.
- Aprovação, rejeição, inclusão de nota e reemissão de token devem falhar quando o estado atual da solicitação não permitir a transição pedida.
- Tokens expirados, inexistentes, já utilizados ou revogados não podem ativar nem validar uma solicitação.
- A rejeição deve exigir motivo legível e a solicitação rejeitada não pode voltar a ser ativável sem uma nova decisão administrativa válida.
- Falhas na criação do evento aprovado ou no vínculo com o usuário não podem deixar a solicitação em estado final inconsistente.
- Usuários sem privilégios administrativos não podem acessar operações administrativas da fila de solicitações.
- O módulo deve continuar respeitando seus limites de desempenho quando houver crescimento de fila, paginação e consultas por filtros.

## Requirements *(obrigatório)*

### Functional Requirements

- **FR-001**: O sistema MUST permitir que um visitante não autenticado envie uma solicitação pública de evento contendo os dados de contato do solicitante, as informações básicas do evento e as capacidades desejadas para o evento.
- **FR-002**: O sistema MUST validar obrigatoriedade, formato e coerência dos dados da solicitação antes de aceitá-la, incluindo e-mail válido, intervalo de datas coerente e tamanhos mínimos razoáveis para os campos textuais essenciais.
- **FR-003**: O sistema MUST criar cada solicitação aceita com um identificador próprio, um código único de acompanhamento e um status inicial de recebida.
- **FR-004**: O sistema MUST impedir a criação de nova solicitação pendente quando já existir solicitação em aberto para a mesma combinação de e-mail do solicitante e nome do evento.
- **FR-005**: O sistema MUST permitir acompanhamento público da solicitação por meio do código de acompanhamento e do e-mail do solicitante, retornando o status atual, o histórico de mudanças e os dados centrais da solicitação.
- **FR-006**: O sistema MUST retornar uma resposta genérica de não encontrado quando a combinação usada no acompanhamento público não corresponder a uma solicitação válida, sem revelar qual dado falhou.
- **FR-007**: O sistema MUST manter o ciclo de vida da solicitação com os estados recebida, em análise, aprovada e recusada, registrando histórico ordenado de mudanças de status e mensagens administrativas relevantes.
- **FR-008**: O sistema MUST oferecer operações administrativas para listar solicitações com paginação, filtro por status, busca textual e consulta detalhada de uma solicitação específica.
- **FR-009**: O sistema MUST permitir que administradores adicionem notas operacionais à solicitação, preservando rastreabilidade e ajustando o estado para análise quando apropriado.
- **FR-010**: O sistema MUST permitir que administradores aprovem uma solicitação elegível, gerando um evento associado em estado pendente de operacionalização e emitindo uma credencial temporária de ativação para o solicitante.
- **FR-011**: O sistema MUST permitir que administradores recusem uma solicitação apenas quando houver um motivo explícito de recusa associado à decisão.
- **FR-012**: O sistema MUST permitir reemissão da credencial de ativação somente para solicitações aprovadas cuja credencial anterior tenha expirado ou se tornado inválida.
- **FR-013**: O sistema MUST disponibilizar uma operação pública para validar a credencial de ativação e informar se a solicitação aprovada ainda pode ser ativada.
- **FR-014**: O sistema MUST permitir que o solicitante conclua a ativação de uma solicitação aprovada, vinculando o evento a uma conta existente elegível ou concluindo a preparação de uma nova conta quando necessário.
- **FR-015**: O sistema MUST impedir reutilização de credenciais de ativação após consumo bem-sucedido.
- **FR-016**: O sistema MUST persistir os dados da solicitação de evento, incluindo dados do solicitante, dados do evento proposto, histórico de status, notas administrativas, motivo de recusa, credencial de ativação, vínculo com evento criado e vínculo com usuário relacionado.
- **FR-017**: O sistema MUST preservar consistência de nomenclatura de endpoints, semântica HTTP e padrões de resposta de erro com os módulos existentes do backend, salvo exceção aprovada e documentada.
- **FR-018**: O sistema MUST definir os testes automatizados necessários para provar a cobertura do envio público, acompanhamento, decisões administrativas, transições inválidas, ativação e cenários de regressão do módulo.
- **FR-019**: O sistema MUST declarar e cumprir expectativas mensuráveis de desempenho para submissão pública, acompanhamento e listagem administrativa.
- **FR-020**: O sistema MUST identificar `mod-event` como módulo dono da funcionalidade, da entidade de solicitação de evento, dos casos de uso de submissão, acompanhamento, revisão e ativação, e dos adapters de entrada e saída necessários para a jornada.
- **FR-021**: O sistema MUST preservar a direção de dependências da Clean Architecture para que regras de domínio da solicitação de evento dependam apenas de abstrações do módulo e não de detalhes de framework, transporte ou persistência.
- **FR-022**: O sistema MUST aderir à arquitetura de banco físico único com separação por schema por módulo e MUST deixar explícito que os contratos de persistência da solicitação de evento pertencem ao módulo dono, com qualquer interação externa ocorrendo por contrato publicado ou serviço exposto.

### Key Entities *(inclua se a funcionalidade envolver dados)*

- **Solicitação de Evento**: Representa o pedido inicial feito por um visitante para criação de um novo evento, com dados do solicitante, dados do evento proposto, status atual, código de acompanhamento, histórico, decisões administrativas e vínculos posteriores.
- **Histórico de Status**: Representa cada mudança relevante sofrida pela solicitação ao longo do fluxo, incluindo o estado assumido, o momento da mudança e a mensagem associada.
- **Credencial de Ativação**: Representa o mecanismo temporário que permite ao solicitante aprovado confirmar a posse da solicitação e assumir o evento.
- **Evento Vinculado**: Representa o evento efetivamente criado a partir de uma solicitação aprovada e ainda pendente de operacionalização completa.

## Success Criteria *(obrigatório)*

### Measurable Outcomes

- **SC-001**: Um visitante consegue concluir o envio de uma nova solicitação de evento e receber um código de acompanhamento em uma única interação, sem autenticação prévia, em 100% dos testes de aceitação do fluxo principal.
- **SC-002**: Um solicitante consegue consultar o status da sua solicitação em menos de 30 segundos de interação humana e obtém retorno conclusivo em pelo menos 95% das tentativas válidas durante os testes de homologação.
- **SC-003**: Operações de submissão pública, acompanhamento e listagem administrativa respondem em até 2 segundos em pelo menos 95% das execuções sob a carga operacional esperada para o módulo.
- **SC-004**: Administradores conseguem decidir uma solicitação elegível, com aprovação ou recusa, em uma única operação por decisão, com 100% das transições inválidas bloqueadas nos cenários de teste documentados.
- **SC-005**: Toda solicitação aprovada gera uma jornada de ativação verificável e toda solicitação recusada permanece inelegível para ativação em 100% dos cenários de aceitação definidos.

## Assumptions

- Os artefatos `data-model-event-requests.md` e `event-requests.openapi.yaml` são a referência canônica para o escopo funcional, nomes de campos e comportamentos esperados desta funcionalidade.
- A funcionalidade será implementada em `mod-event`, e a lógica de negócio principal da solicitação de eventos permanecerá nesse módulo.
- O backend já possui uma capacidade existente de autenticação e gestão de contas que pode ser reutilizada quando a ativação precisar vincular ou preparar um usuário.
- Endpoints públicos de solicitação, acompanhamento e ativação não exigem autenticação prévia; endpoints administrativos exigem privilégios administrativos válidos.
- A aprovação cria um evento vinculado ainda sujeito ao ciclo normal de operacionalização do produto, começando em estado pendente de uso completo.
- Notificações ao solicitante podem reutilizar a infraestrutura de comunicação já adotada pelo produto, sem alterar o comportamento funcional principal da API quando houver modo local ou ambiente de teste.
- A funcionalidade continuará usando o banco físico único existente, com schema controlado pelo módulo dono e sem acesso direto ao schema de outros módulos.
- O orçamento de desempenho será validado no ambiente atual do projeto com massa de dados representativa da fila esperada de solicitações.
- Documentação operacional e de consumo da API será atualizada se a superfície pública ou administrativa divergir do estado atual do repositório.
