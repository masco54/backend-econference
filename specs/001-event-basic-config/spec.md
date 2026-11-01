# Especificação de Funcionalidade: Atualização de Configuração Básica do Evento

**Feature Branch**: `001-event-basic-config`  
**Created**: 2026-07-13  
**Status**: Draft  
**Input**: Descrição do usuário: "preciso de duas specs uma de front e outra de backend para fazer funcionar a configuração básica do evento. A spec vai se limitar somente a atualização das configurações básicas do evento."

## User Scenarios & Testing *(obrigatório)*

### User Story 1 - Organizador consulta a configuração básica atual (Priority: P1)

O backend retorna os dados básicos atuais de um evento para que a tela de Configuração Básica seja preenchida corretamente antes da edição.

**Why this priority**: A atualização segura depende da leitura consistente dos dados atuais; sem isso o frontend não consegue exibir o formulário de edição com confiança.

**Independent Test**: Chamar a fronteira HTTP de detalhe/configuração básica do evento como organizador autorizado e verificar resposta com nome, sigla, descrição, tipo, tema, datas e localização.

**Acceptance Scenarios**:

1. **Given** um organizador autenticado dono do evento, **When** consulta a configuração básica do evento, **Then** recebe 200 com todos os campos básicos atuais.
2. **Given** um evento com campos opcionais nulos, **When** a configuração básica é consultada, **Then** a resposta mantém os campos opcionais como nulos ou vazios sem erro.
3. **Given** um usuário sem autenticação, **When** consulta a configuração básica, **Then** recebe 401.
4. **Given** um organizador sem acesso ao evento, **When** consulta a configuração básica, **Then** recebe 403 ou 404 conforme padrão de segurança existente.

---

### User Story 2 - Organizador atualiza a configuração básica (Priority: P1)

O backend aceita alterações válidas nos dados básicos do evento e persiste a nova configuração sem modificar outras áreas da gestão do evento.

**Why this priority**: É o comportamento central solicitado. A feature só entrega valor quando as alterações básicas são persistidas e retornadas ao frontend.

**Independent Test**: Enviar uma atualização válida para a API de evento básico como organizador autorizado, consultar novamente o evento e verificar que os novos valores foram persistidos.

**Acceptance Scenarios**:

1. **Given** um organizador autorizado e dados básicos válidos, **When** envia a atualização, **Then** recebe 200 com a configuração atualizada.
2. **Given** a atualização altera apenas nome e localização, **When** o backend processa a requisição, **Then** somente os campos básicos enviados são alterados e demais configurações permanecem inalteradas.
3. **Given** campos opcionais são enviados como vazios ou nulos, **When** a atualização é aceita, **Then** esses campos são persistidos como vazios ou nulos conforme contrato.

---

### User Story 3 - Backend valida dados e permissões (Priority: P2)

O backend rejeita atualizações inválidas ou não autorizadas com respostas consistentes, sem persistir dados parciais.

**Why this priority**: Validação e autorização protegem a consistência do domínio e impedem que organizadores alterem eventos fora do seu escopo.

**Independent Test**: Enviar payloads inválidos e requisições sem permissão para a fronteira HTTP de atualização e verificar status, mensagem e ausência de alteração persistida.

**Acceptance Scenarios**:

1. **Given** o nome do evento está vazio, **When** a atualização é enviada, **Then** o backend retorna 400 e não altera o evento.
2. **Given** a data final é anterior à data inicial, **When** a atualização é enviada, **Then** o backend retorna 400 e não altera o evento.
3. **Given** um organizador sem acesso ao evento, **When** envia uma atualização, **Then** o backend retorna 403 ou 404 e não altera o evento.
4. **Given** um evento finalizado ou cancelado, **When** o organizador tenta alterar a configuração básica, **Then** o backend rejeita a alteração para preservar o histórico do evento.

---

### Edge Cases

- O evento pode não existir; a API deve retornar erro padronizado de recurso não encontrado.
- Campos opcionais como sigla, descrição, tema e logo podem ser nulos sem invalidar a configuração.
- Datas podem ser omitidas apenas se o contrato atual já permitir evento sem datas; quando ambas existem, a data final não pode ser anterior à inicial.
- O usuário pode estar autenticado, mas não possuir papel de organizador/admin; a API deve negar acesso.
- Uma tentativa de atualização inválida não pode persistir parte dos campos antes de falhar.
- A funcionalidade não pode alterar categorias, eixos temáticos, modalidades, inscrições, submissões, avaliação, estatísticas ou publicação.

## Requirements *(obrigatório)*

### Functional Requirements

- **FR-001**: O sistema MUST fornecer uma forma de consultar os dados básicos atuais de um evento acessível ao organizador autorizado.
- **FR-002**: O sistema MUST permitir atualização dos campos básicos: nome, sigla, descrição, tipo, tema, data inicial, data final e localização.
- **FR-003**: O sistema MAY aceitar `logoUrl` como campo opcional de configuração básica quando o contrato existente de evento já o expõe; upload de arquivo está fora do escopo.
- **FR-004**: O sistema MUST restringir leitura e atualização ao organizador principal autorizado e administradores, preservando o padrão existente de autenticação e autorização.
- **FR-005**: O sistema MUST rejeitar atualização sem nome de evento válido.
- **FR-006**: O sistema MUST rejeitar atualização quando data final for anterior à data inicial.
- **FR-007**: O sistema MUST retornar a representação atualizada do evento após atualização bem-sucedida.
- **FR-008**: O sistema MUST preservar campos e relacionamentos fora do escopo quando atualizar a configuração básica.
- **FR-009**: O sistema MUST não criar nem alterar categorias de inscrição, eixos temáticos, modalidades, inscrições, submissões, avaliação, programação, estatísticas ou publicação nesta feature.
- **FR-010**: O sistema MUST usar respostas de erro consistentes com os módulos existentes para 400, 401, 403 e 404.
- **FR-011**: O sistema MUST definir testes automatizados cobrindo sucesso, validação, autorização e inexistência do evento.
- **FR-012**: O sistema MUST preservar consistência de nomenclatura de endpoints, semântica HTTP e padrões de resposta de erro com os módulos existentes do backend, salvo exceção aprovada e documentada.
- **FR-013**: O sistema MUST declarar expectativas mensuráveis de performance para consulta e atualização da configuração básica.
- **FR-014**: O sistema MUST identificar `mod-event` como módulo dono do modelo de domínio, casos de uso, ports/adapters e contratos de persistência afetados.
- **FR-015**: O sistema MUST preservar a direção de dependências da Clean Architecture para que o domínio dependa apenas de abstrações possuídas pelo módulo.
- **FR-016**: O sistema MUST aderir à arquitetura de dados de banco físico único; a funcionalidade MUST NOT introduzir banco físico dedicado por módulo.

### Key Entities *(inclua se a funcionalidade envolver dados)*

- **Event**: Entidade central do evento, dona dos dados básicos editáveis e do vínculo com o organizador.
- **EventBasicConfigRequest**: Dados recebidos para alteração básica do evento, limitados aos campos desta feature.
- **EventBasicConfigResponse**: Dados retornados para preencher o formulário e confirmar atualização.
- **EventAccessRule**: Regra de autorização que determina se o usuário autenticado pode consultar ou atualizar o evento.

## Success Criteria *(obrigatório)*

### Measurable Outcomes

- **SC-001**: Consulta de configuração básica retorna os dados necessários para preencher o formulário em menos de 200ms no p95 local/integrado.
- **SC-002**: Atualização válida de configuração básica retorna sucesso e dados atualizados em menos de 300ms no p95 local/integrado.
- **SC-003**: 100% dos testes automatizados planejados para sucesso, validação, autorização e evento inexistente passam.
- **SC-004**: Requisições inválidas não alteram dados persistidos em 100% dos cenários de validação testados.
- **SC-005**: Nenhuma operação desta feature altera entidades fora dos dados básicos do evento.

## Assumptions

- A funcionalidade será implementada em `mod-event`, que é dono da entidade `Event` e do schema/tabela de eventos.
- A autenticação por cookie JWT e os papéis existentes serão reutilizados.
- Eventos finalizados ou cancelados não devem aceitar edição de configuração básica.
- Co-organizadores podem ser contemplados em expansão futura; para esta spec, o acesso mínimo esperado é organizador principal e admin.
- A feature usa o banco físico único existente e migrations do módulo dono quando campos básicos ainda não existirem.
- Integrações com outros módulos não são necessárias para este escopo; qualquer necessidade futura deve ocorrer via contrato publicado.
