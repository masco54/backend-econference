# Tasks: Event Manage API

**Input**: Design documents from `/specs/005-event-manage-api/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/event-manage.openapi.yaml, quickstart.md

**Tests**: Automated tests are required by FR-011 and must be created before implementation tasks in each story phase.

**Organization**: Tasks are grouped by user story so each story can be implemented and tested independently.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare contract-first implementation baseline for mod-event.

- [X] T001 Align endpoint and schema expectations in back-econference/specs/005-event-manage-api/contracts/event-manage.openapi.yaml with back-econference/specs/005-event-manage-api/spec.md
- [X] T002 Map impacted module files and package boundaries in back-econference/specs/005-event-manage-api/plan.md
- [X] T003 [P] Prepare migration ordering notes for V8/V9 in back-econference/specs/005-event-manage-api/data-model.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core persistence and access-control foundations needed by all stories.

**CRITICAL**: No user story work starts before this phase is complete.

- [X] T004 Create migration for new event configuration columns in back-econference/application/src/main/resources/db/migration/V8__add_event_config_fields.sql
- [X] T005 Create migration for event configuration tables in back-econference/application/src/main/resources/db/migration/V9__create_event_config_tables.sql
- [X] T006 [P] Expand Event aggregate fields and defaults in back-econference/mod-event/src/main/java/com/econference/event/domain/Event.java
- [X] T007 [P] Add ownership-aware event lookup method in back-econference/mod-event/src/main/java/com/econference/event/repository/EventRepository.java
- [X] T008 Implement shared organizer/admin authorization check helper in back-econference/mod-event/src/main/java/com/econference/event/application/service/OrganizerEventService.java
- [X] T009 [P] Extend summary DTO contract for shared fields in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventSummaryResponse.java

**Checkpoint**: Foundation ready for user stories.

---

## Phase 3: User Story 1 - Organizer gets full event detail (Priority: P1) MVP

**Goal**: Implement GET /api/v1/events/{id} returning full EventDetailResponse with proper authorization.

**Independent Test**: Authenticated organizer requests own event and receives 200 + full payload; foreign organizer and unauthenticated requests are denied.

### Tests for User Story 1

- [X] T010 [P] [US1] Add integration test for GET /api/v1/events/{id} success case in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerIT.java
- [X] T011 [P] [US1] Add integration test for GET /api/v1/events/{id} foreign organizer forbidden/not-found in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerSecurityIT.java
- [X] T012 [P] [US1] Add integration test for GET /api/v1/events/{id} unauthenticated 401 in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerSecurityIT.java

### Implementation for User Story 1

- [X] T013 [P] [US1] Create event detail response DTO in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventDetailResponse.java
- [X] T014 [P] [US1] Add event detail use case port in back-econference/mod-event/src/main/java/com/econference/event/application/port/in/GetEventDetailUseCase.java
- [X] T015 [US1] Implement organizer event detail service in back-econference/mod-event/src/main/java/com/econference/event/application/service/OrganizerEventDetailService.java
- [X] T016 [US1] Expose GET /api/v1/events/{id} endpoint in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/EventController.java
- [X] T017 [US1] Add exception mapping for event not found/forbidden in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/EventController.java

**Checkpoint**: User Story 1 works independently.

---

## Phase 4: User Story 2 - Event list returns complete summary fields (Priority: P1)

**Goal**: Expand GET /api/v1/events summary payload with type, theme, logoUrl, configurationCompleted.

**Independent Test**: Organizer list endpoint returns new fields for each event item, including false defaults for configurationCompleted.

### Tests for User Story 2

- [ ] T018 [P] [US2] Add integration test asserting expanded list summary fields in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerIT.java
- [ ] T019 [P] [US2] Add service-level test for configurationCompleted default false in back-econference/mod-event/src/test/java/com/econference/event/application/service/OrganizerEventServiceTest.java

### Implementation for User Story 2

- [ ] T020 [US2] Expand event summary mapping in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/EventController.java
- [ ] T021 [US2] Populate new summary fields from Event domain in back-econference/mod-event/src/main/java/com/econference/event/application/service/OrganizerEventService.java
- [ ] T022 [US2] Ensure null-safe serialization of optional type/theme/logoUrl in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventSummaryResponse.java

**Checkpoint**: User Story 2 works independently.

---

## Phase 5: User Story 3 - Event configuration endpoints (Priority: P2)

**Goal**: Implement read endpoints for registration categories, theme axes, and submission modalities with event access control.

**Independent Test**: Organizer calls each endpoint for own event and gets 200 (empty or populated list); unauthorized organizer receives 403/404.

### Tests for User Story 3

- [ ] T023 [P] [US3] Add integration tests for categories/theme-axes/modalities success in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerIT.java
- [ ] T024 [P] [US3] Add integration tests for unauthorized access on config endpoints in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerSecurityIT.java

### Implementation for User Story 3

- [ ] T025 [P] [US3] Create RegistrationCategory entity in back-econference/mod-event/src/main/java/com/econference/event/domain/RegistrationCategory.java
- [ ] T026 [P] [US3] Create ThemeAxis entity in back-econference/mod-event/src/main/java/com/econference/event/domain/ThemeAxis.java
- [ ] T027 [P] [US3] Create SubmissionModality entity in back-econference/mod-event/src/main/java/com/econference/event/domain/SubmissionModality.java
- [ ] T028 [P] [US3] Create repositories for config entities in back-econference/mod-event/src/main/java/com/econference/event/repository/RegistrationCategoryRepository.java and back-econference/mod-event/src/main/java/com/econference/event/repository/ThemeAxisRepository.java and back-econference/mod-event/src/main/java/com/econference/event/repository/SubmissionModalityRepository.java
- [ ] T029 [P] [US3] Create services for config entities in back-econference/mod-event/src/main/java/com/econference/event/application/service/RegistrationCategoryService.java and back-econference/mod-event/src/main/java/com/econference/event/application/service/ThemeAxisService.java and back-econference/mod-event/src/main/java/com/econference/event/application/service/SubmissionModalityService.java
- [ ] T030 [US3] Add EventConfigController endpoints for categories/theme-axes/modalities in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/EventConfigController.java

**Checkpoint**: User Story 3 works independently.

---

## Phase 6: User Story 4 - Event dashboard statistics endpoint (Priority: P2)

**Goal**: Implement GET /api/v1/events/{id}/statistics with access control and stable aggregate structure.

**Independent Test**: Organizer requests statistics endpoint and receives 200 with registrationsCount, submissionsCount, reviewersCount, and publishedCount values.

### Tests for User Story 4

- [ ] T031 [P] [US4] Add integration test for statistics success payload in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerIT.java
- [ ] T032 [P] [US4] Add integration test for statistics unauthorized access in back-econference/mod-event/src/test/java/com/econference/event/adapter/in/web/EventControllerSecurityIT.java

### Implementation for User Story 4

- [ ] T033 [P] [US4] Create statistics response DTO in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/dto/response/EventStatisticsResponse.java
- [ ] T034 [US4] Implement statistics aggregation service in back-econference/mod-event/src/main/java/com/econference/event/application/service/EventStatisticsService.java
- [ ] T035 [US4] Add GET /api/v1/events/{id}/statistics endpoint in back-econference/mod-event/src/main/java/com/econference/event/adapter/in/web/EventConfigController.java

**Checkpoint**: User Story 4 works independently.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Final hardening, validation, and docs across all stories.

- [ ] T036 [P] Update API contract examples and response schemas in back-econference/specs/005-event-manage-api/contracts/event-manage.openapi.yaml
- [ ] T037 [P] Update execution and validation instructions in back-econference/specs/005-event-manage-api/quickstart.md
- [ ] T038 Validate Flyway + module test suite execution path in back-econference/specs/005-event-manage-api/plan.md using mvn -pl mod-event test and mvn -pl application test

---

## Dependencies & Execution Order

### Phase Dependencies

- Setup (Phase 1): starts immediately.
- Foundational (Phase 2): depends on Setup and blocks all stories.
- User Stories (Phase 3+): depend on Foundational completion.
- Polish (Phase 7): depends on completion of selected stories.

### User Story Dependencies

- US1 (P1): starts after Phase 2; no dependency on other stories.
- US2 (P1): starts after Phase 2; can run in parallel with US1 after DTO/domain baseline merges.
- US3 (P2): starts after Phase 2; independent from US4 except shared EventConfigController merge order.
- US4 (P2): starts after Phase 2; can run in parallel with US3 if endpoint wiring is coordinated.

### Parallel Opportunities

- T003 can run with T001/T002.
- T006, T007, T009 can run in parallel.
- T010/T011/T012 can run in parallel.
- T025/T026/T027 and T028/T029 can run in parallel.
- T031 and T032 can run in parallel.
- T036 and T037 can run in parallel.

---

## Parallel Example: User Story 3

- Task: T025 Create RegistrationCategory entity in back-econference/mod-event/src/main/java/com/econference/event/domain/RegistrationCategory.java
- Task: T026 Create ThemeAxis entity in back-econference/mod-event/src/main/java/com/econference/event/domain/ThemeAxis.java
- Task: T027 Create SubmissionModality entity in back-econference/mod-event/src/main/java/com/econference/event/domain/SubmissionModality.java

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 and Phase 2.
2. Complete Phase 3 (US1) including tests.
3. Validate US1 independently.

### Incremental Delivery

1. Deliver US1 to unblock manage route loading.
2. Deliver US2 to fix list payload completeness.
3. Deliver US3 to enable manage configuration sections.
4. Deliver US4 for dashboard metrics.
5. Run Phase 7 hardening and validation.
