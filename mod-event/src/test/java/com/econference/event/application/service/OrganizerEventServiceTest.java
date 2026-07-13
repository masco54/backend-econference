package com.econference.event.application.service;

import com.econference.event.application.port.in.GetOrganizerEventsUseCase;
import com.econference.event.domain.Event;
import com.econference.event.domain.EventStatus;
import com.econference.event.service.EventService;
import com.econference.shared.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizerEventServiceTest {

    @Mock
    private EventService eventService;

    @InjectMocks
    private OrganizerEventService organizerEventService;

    private final UUID organizerId = UUID.randomUUID();

    @Test
    void shouldReturnEventsForOrganizer() {
        Event event = Event.builder().name("SBBD 2025").organizerId(organizerId).build();
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1);
        when(eventService.findByOrganizerPaged(eq(organizerId), any(Pageable.class))).thenReturn(page);

        GetOrganizerEventsUseCase.OrganizerListResult result = organizerEventService.get(
                new GetOrganizerEventsUseCase.OrganizerListCommand(organizerId, null, 1, 20));

        assertThat(result.data()).hasSize(1);
        assertThat(result.data().get(0).getName()).isEqualTo("SBBD 2025");
        assertThat(result.pagination().total()).isEqualTo(1);
        assertThat(result.pagination().page()).isEqualTo(1);
    }

    @Test
    void shouldReturnEmptyListWhenOrganizerHasNoEvents() {
        Page<Event> empty = Page.empty(PageRequest.of(0, 20));
        when(eventService.findByOrganizerPaged(eq(organizerId), any(Pageable.class))).thenReturn(empty);

        GetOrganizerEventsUseCase.OrganizerListResult result = organizerEventService.get(
                new GetOrganizerEventsUseCase.OrganizerListCommand(organizerId, null, 1, 20));

        assertThat(result.data()).isEmpty();
        assertThat(result.pagination().total()).isZero();
    }

    @Test
    void shouldFilterByStatusWhenProvided() {
        Event event = Event.builder().name("SBBD 2025").organizerId(organizerId).status(EventStatus.PUBLISHED).build();
        Page<Event> page = new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1);
        when(eventService.findByOrganizerAndStatusPaged(eq(organizerId), eq(EventStatus.PUBLISHED), any(Pageable.class)))
                .thenReturn(page);

        GetOrganizerEventsUseCase.OrganizerListResult result = organizerEventService.get(
                new GetOrganizerEventsUseCase.OrganizerListCommand(organizerId, "PUBLISHED", 1, 20));

        assertThat(result.data()).hasSize(1);
        verify(eventService).findByOrganizerAndStatusPaged(eq(organizerId), eq(EventStatus.PUBLISHED), any(Pageable.class));
    }

    @Test
    void shouldThrowBusinessExceptionForInvalidStatus() {
        assertThatThrownBy(() -> organizerEventService.get(
                new GetOrganizerEventsUseCase.OrganizerListCommand(organizerId, "INVALID_STATUS", 1, 20)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("INVALID_STATUS");
    }

    @Test
    void shouldNotReturnEventsFromAnotherOrganizer() {
        UUID otherOrganizerId = UUID.randomUUID();
        Event otherEvent = Event.builder().name("Other Event").organizerId(otherOrganizerId).build();
        Page<Event> myPage = Page.empty(PageRequest.of(0, 20));
        when(eventService.findByOrganizerPaged(eq(organizerId), any(Pageable.class))).thenReturn(myPage);

        GetOrganizerEventsUseCase.OrganizerListResult result = organizerEventService.get(
                new GetOrganizerEventsUseCase.OrganizerListCommand(organizerId, null, 1, 20));

        assertThat(result.data()).doesNotContain(otherEvent);
    }

    @Test
    void shouldApplyPageSortByCreatedAtDesc() {
        Page<Event> page = Page.empty(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));
        when(eventService.findByOrganizerPaged(eq(organizerId), any(Pageable.class))).thenReturn(page);

        organizerEventService.get(new GetOrganizerEventsUseCase.OrganizerListCommand(organizerId, null, 1, 20));

        verify(eventService).findByOrganizerPaged(eq(organizerId),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }
}
