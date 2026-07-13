# Research: Listagem de Eventos do Organizador

**Feature**: `003-organizer-event-listing`  
**Date**: 2026-07-12  
**Status**: ✅ Complete — nenhum NEEDS CLARIFICATION em aberto

---

## 1. Como extrair o ID do organizador autenticado

**Decision**: Usar `@AuthenticationPrincipal AuthenticatedUserPrincipal principal` e chamar `principal.getUserId()`.  
**Rationale**: `AuthenticatedUserPrincipal` (em `mod-auth`) já expõe `getUserId(): UUID`. O padrão `@AuthenticationPrincipal(expression = "userId")` também funciona e é usado em `EventRequestAdminController`; ambas as formas são equivalentes — usar a expressão SpEL é mais concisa e consistente com o código existente.  
**Alternatives considered**: Injetar `SecurityContextHolder` diretamente no service — rejeitado por introduzir dependência de framework na camada de aplicação, violando a constituição.

---

## 2. Autorização: qual role é necessária

**Decision**: `@PreAuthorize("hasRole('ORGANIZER')")` no método do controller.  
**Rationale**: `AuthenticatedUserPrincipal.getAuthorities()` gera authorities com prefixo `ROLE_` a partir de `UserRole.ORGANIZER`. O padrão `hasRole('ADMIN')` em `EventRequestAdminController` confirma a convenção. Um organizador sem a role `ORGANIZER` não deve enxergar este endpoint.  
**Alternatives considered**: `@Authenticated` sem verificação de role — rejeitado porque qualquer usuário logado (participante, revisor) poderia chamar o endpoint e receber lista vazia em vez de 403, o que seria enganoso e inconsistente com a spec.

---

## 3. Paginação no repositório existente

**Decision**: Adicionar `Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable)` ao `EventRepository` (Spring Data JPA).  
**Rationale**: O método `List<Event> findByOrganizerId(UUID organizerId)` já existe mas não suporta paginação. Spring Data JPA deriva a query automaticamente a partir do nome do método; adicionar uma sobrecarga com `Pageable` é zero-config.  
**Alternatives considered**: Query manual com `@Query` — desnecessário; `Specification` — overhead injustificado para um filtro simples.

---

## 4. Filtro por status

**Decision**: Adicionar `Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatus status, Pageable pageable)` ao `EventRepository` e delegar a seleção do método ao service.  
**Rationale**: Spring Data JPA deriva a query automaticamente. O service escolhe qual método chamar com base na presença/ausência do parâmetro `status`.  
**Alternatives considered**: `Specification` — útil para filtros compostos, mas esta feature tem apenas dois campos opcionais (status já cobre 100% do requisito); Querydsl — dependência adicional não justificada.

---

## 5. Envelope de resposta e paginação

**Decision**: Reutilizar o mesmo padrão `record` existente: `OrganizerEventsResponse(List<EventSummaryResponse> data, PaginationResponse pagination)`.  
**Rationale**: `AdminEventRequestListResponse` usa exatamente este shape. O frontend espera `{ data: [...], pagination: { page, limit, total, totalPages } }` conforme contrato OpenAPI já gerado. Reutilizar o padrão mantém consistência (princípio III da constituição).  
**Alternatives considered**: Usar um tipo genérico `PagedResponse<T>` em `shared` — válido para o futuro, mas não há segundo módulo usando o shape ainda; prematuro neste momento.

---

## 6. Localização do novo controller handler

**Decision**: Adicionar o método `GET /api/v1/events` ao `EventController` existente (pacote `com.econference.event.controller`).  
**Rationale**: `EventController` já está mapeado em `/api/v1/events` e tem o health check. É o lugar semanticamente correto.  
**Alternatives considered**: Criar `OrganizerEventController` separado — desnecessário; o controller não seria grande o suficiente para justificar divisão.

---

## 7. Campos do DTO de resposta (`EventSummaryResponse`)

**Decision**: `id`, `name`, `status`, `startDate`, `endDate`, `location`, `createdAt`.  
**Rationale**: Cobre todos os campos que o frontend exibe na listagem (`MyEventsComponent`) conforme levantado na spec do front (028). `description` é omitido por ser potencialmente longo; não é exibido na listagem.  
**Alternatives considered**: Retornar a entidade completa — violaria separação de responsabilidades e exporia campos internos.

---

## 8. Sem migrações necessárias

**Decision**: Nenhuma migração de schema.  
**Rationale**: A tabela `events` já existe com todos os campos necessários. O novo endpoint apenas lê dados existentes.

