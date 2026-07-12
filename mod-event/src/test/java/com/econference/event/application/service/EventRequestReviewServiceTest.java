package com.econference.event.application.service;

import com.econference.event.application.exception.InvalidEventRequestStateException;
import com.econference.event.application.port.in.AddEventRequestNoteUseCase;
import com.econference.event.application.port.in.ApproveEventRequestUseCase;
import com.econference.event.application.port.in.ReissueActivationTokenUseCase;
import com.econference.event.application.port.in.RejectEventRequestUseCase;
import com.econference.event.application.port.out.EventRequestNotificationPort;
import com.econference.event.application.port.out.EventRequestRepositoryPort;
import com.econference.event.config.EventRequestConfig;
import com.econference.event.domain.Event;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import com.econference.event.domain.EventStatus;
import com.econference.event.service.EventService;
import com.econference.shared.contract.OrganizerActivationTokenPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRequestReviewServiceTest {

    @Mock
    private EventRequestRepositoryPort eventRequestRepositoryPort;

    @Mock
    private EventRequestNotificationPort eventRequestNotificationPort;

    @Mock
    private EventService eventService;

    @Mock
    private OrganizerActivationTokenPort organizerActivationTokenPort;

    @InjectMocks
    private EventRequestReviewService eventRequestReviewService;

    @BeforeEach
    void setUp() {
        EventRequestConfig config = new EventRequestConfig();
        config.setActivationExpiryHours(168);
        config.setBaseUrl("http://localhost:4200");
        config.setActivationPath("/event-request/activate");
        eventRequestReviewService = new EventRequestReviewService(
                eventRequestRepositoryPort,
                eventRequestNotificationPort,
                eventService,
                config,
                organizerActivationTokenPort
        );
    }

    @Test
    void shouldMoveRequestedRequestToUnderReviewWhenAddingNote() {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = requestedRequest();

        when(eventRequestRepositoryPort.findDetailedById(request.getId())).thenReturn(Optional.of(request));
        when(eventRequestRepositoryPort.save(any(EventRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventRequest result = eventRequestReviewService.addNote(new AddEventRequestNoteUseCase.AddNoteCommand(
                request.getId(),
                "Precisamos confirmar o cronograma final.",
                adminUserId
        ));

        assertThat(result.getStatus()).isEqualTo(EventRequestStatus.UNDER_REVIEW);
        assertThat(result.getAdminNotes()).isEqualTo("Precisamos confirmar o cronograma final.");
        assertThat(result.getStatusHistory()).hasSize(2);
        assertThat(result.getStatusHistory().getLast().getStatus()).isEqualTo(EventRequestStatus.UNDER_REVIEW);
        verify(eventRequestNotificationPort).sendNoteAdded(result, "Precisamos confirmar o cronograma final.");
    }

    @Test
    void shouldApproveEligibleRequestAndCreatePendingEvent() {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = underReviewRequest();
        Event createdEvent = Event.builder()
                .name(request.getEventName())
                .description(request.getEventTheme())
                .startDate(request.getEventStartDate())
                .endDate(request.getEventEndDate())
                .location(request.getEventLocation())
                .organizerId(adminUserId)
                .status(EventStatus.PENDING)
                .build();
        setEntityField(createdEvent, "id", UUID.fromString("22222222-2222-2222-2222-222222222222"));

        when(eventRequestRepositoryPort.findDetailedById(request.getId())).thenReturn(Optional.of(request));
        when(eventService.createPendingOperationalEvent(
                request.getEventName(),
                request.getEventTheme(),
                request.getEventStartDate(),
                request.getEventEndDate(),
                request.getEventLocation(),
                adminUserId
        )).thenReturn(createdEvent);
        when(eventRequestRepositoryPort.save(any(EventRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventRequest result = eventRequestReviewService.approve(new ApproveEventRequestUseCase.ApproveCommand(
                request.getId(),
                "Solicitação aderente ao calendário institucional.",
                adminUserId
        ));

        assertThat(result.getStatus()).isEqualTo(EventRequestStatus.APPROVED);
        assertThat(result.getLinkedEventId()).isEqualTo(createdEvent.getId());
        assertThat(result.getActivationToken()).isNotNull();
        assertThat(result.getActivationTokenExpiresAt()).isAfter(Instant.now());
        verify(eventRequestNotificationPort).sendApproval(result, "http://localhost:4200/register?token=" + result.getActivationToken());
    }

    @Test
    void shouldRejectEligibleRequestWithReason() {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = requestedRequest();

        when(eventRequestRepositoryPort.findDetailedById(request.getId())).thenReturn(Optional.of(request));
        when(eventRequestRepositoryPort.save(any(EventRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventRequest result = eventRequestReviewService.reject(new RejectEventRequestUseCase.RejectCommand(
                request.getId(),
                "O período informado conflita com eventos já homologados.",
                adminUserId
        ));

        assertThat(result.getStatus()).isEqualTo(EventRequestStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo("O período informado conflita com eventos já homologados.");
        assertThat(result.getStatusHistory().getLast().getStatus()).isEqualTo(EventRequestStatus.REJECTED);
        verify(eventRequestNotificationPort).sendRejection(result);
    }

    @Test
    void shouldReissueExpiredActivationTokenForApprovedRequest() {
        UUID adminUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        EventRequest request = approvedRequest();
        request.setActivationTokenExpiresAt(Instant.now().minusSeconds(60));

        when(eventRequestRepositoryPort.findDetailedById(request.getId())).thenReturn(Optional.of(request));
        when(eventRequestRepositoryPort.save(any(EventRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EventRequest result = eventRequestReviewService.reissue(
                new ReissueActivationTokenUseCase.ReissueCommand(request.getId(), adminUserId)
        );

        assertThat(result.getStatus()).isEqualTo(EventRequestStatus.APPROVED);
        assertThat(result.getActivationToken()).isNotNull().isNotEqualTo(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        assertThat(result.getActivationTokenExpiresAt()).isAfter(Instant.now());
        verify(eventRequestNotificationPort).sendActivationTokenReissued(result, "http://localhost:4200/register?token=" + result.getActivationToken());
    }

    @Test
    void shouldRejectReissueWhenApprovedRequestStillHasValidToken() {
        EventRequest request = approvedRequest();

        when(eventRequestRepositoryPort.findDetailedById(request.getId())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> eventRequestReviewService.reissue(
                new ReissueActivationTokenUseCase.ReissueCommand(request.getId(), UUID.randomUUID())
        )).isInstanceOf(InvalidEventRequestStateException.class)
                .hasMessageContaining("cannot be reissued");
    }

    private EventRequest requestedRequest() {
        EventRequest request = baseRequest(EventRequestStatus.REQUESTED);
        request.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        return request;
    }

    private EventRequest underReviewRequest() {
        EventRequest request = baseRequest(EventRequestStatus.UNDER_REVIEW);
        request.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        request.addHistoryEntry(EventRequestStatus.UNDER_REVIEW, "Solicitação em análise", UUID.fromString("77777777-7777-7777-7777-777777777777"));
        return request;
    }

    private EventRequest approvedRequest() {
        EventRequest request = baseRequest(EventRequestStatus.APPROVED);
        request.setActivationToken(UUID.fromString("66666666-6666-6666-6666-666666666666"));
        request.setActivationTokenExpiresAt(Instant.now().plusSeconds(3600));
        request.setLinkedEventId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
        request.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        request.addHistoryEntry(EventRequestStatus.APPROVED, "Approved by admin", UUID.fromString("77777777-7777-7777-7777-777777777777"));
        return request;
    }

    private EventRequest baseRequest(EventRequestStatus status) {
        EventRequest request = EventRequest.builder()
                .trackingCode("SOL-2026-A3F8")
                .status(status)
                .requesterFirstName("Ana")
                .requesterLastName("Silva")
                .requesterName("Ana Silva")
                .requesterEmail("ana.silva@example.com")
                .requesterPhone("+5511999999999")
                .requesterInstitution("UFSC")
                .eventName("Congresso Brasileiro de IA")
                .eventTheme("Aplicações de IA na pesquisa científica")
                .eventStartDate(LocalDate.of(2026, 9, 10))
                .eventEndDate(LocalDate.of(2026, 9, 12))
                .eventLocation("Florianópolis")
                .eventType("conference")
                .hasRegistration(true)
                .hasSubmission(true)
                .hasSite(true)
                .build();
        setEntityField(request, "id", UUID.fromString("99999999-9999-9999-9999-999999999999"));
        setEntityField(request, "createdAt", Instant.parse("2026-04-01T10:15:30Z"));
        return request;
    }

    private void setEntityField(Object target, String fieldName, Object value) {
        try {
            Class<?> current = target.getClass();
            while (current != null) {
                try {
                    var field = current.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (NoSuchFieldException ignored) {
                    current = current.getSuperclass();
                }
            }
            throw new NoSuchFieldException(fieldName);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}