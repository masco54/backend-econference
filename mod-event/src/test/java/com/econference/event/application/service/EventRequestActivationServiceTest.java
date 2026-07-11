package com.econference.event.application.service;

import com.econference.event.application.exception.InvalidActivationTokenException;
import com.econference.event.application.port.in.ActivateEventRequestUseCase;
import com.econference.event.application.port.in.TrackEventRequestUseCase;
import com.econference.event.application.port.in.ValidateActivationTokenUseCase;
import com.econference.event.application.port.out.EventRequestRepositoryPort;
import com.econference.event.config.EventRequestConfig;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import com.econference.event.service.EventService;
import com.econference.shared.contract.EventRequestAccountProvisioningPort;
import com.econference.shared.exception.BusinessException;
import com.econference.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRequestActivationServiceTest {

    @Mock
    private EventRequestRepositoryPort eventRequestRepositoryPort;

    @Mock
    private EventRequestAccountProvisioningPort eventRequestAccountProvisioningPort;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventRequestActivationService eventRequestActivationService;

    @Test
    void shouldReturnActivationUrlWhenApprovedRequestHasValidToken() {
        EventRequestConfig eventRequestConfig = new EventRequestConfig();
        TrackEventRequestService trackEventRequestService = new TrackEventRequestService(eventRequestRepositoryPort, eventRequestConfig);
        EventRequest request = approvedRequest();

        when(eventRequestRepositoryPort.findByTrackingCodeAndRequesterEmail("SOL-2026-A3F8", "ana.silva@example.com"))
                .thenReturn(Optional.of(request));

        TrackEventRequestUseCase.TrackResult result = trackEventRequestService.track(
                new TrackEventRequestUseCase.TrackCommand("SOL-2026-A3F8", "ana.silva@example.com")
        );

        assertThat(result.activationUrl()).isEqualTo("http://localhost:4200/register?token=11111111-1111-1111-1111-111111111111");
    }

    @Test
    void shouldHideActivationUrlWhenTokenHasExpired() {
        EventRequestConfig eventRequestConfig = new EventRequestConfig();
        TrackEventRequestService trackEventRequestService = new TrackEventRequestService(eventRequestRepositoryPort, eventRequestConfig);
        EventRequest request = approvedRequest();
        request.setActivationTokenExpiresAt(Instant.now().minusSeconds(60));

        when(eventRequestRepositoryPort.findByTrackingCodeAndRequesterEmail("SOL-2026-A3F8", "ana.silva@example.com"))
                .thenReturn(Optional.of(request));

        TrackEventRequestUseCase.TrackResult result = trackEventRequestService.track(
                new TrackEventRequestUseCase.TrackCommand("SOL-2026-A3F8", "ana.silva@example.com")
        );

        assertThat(result.activationUrl()).isNull();
    }

    @Test
    void shouldRejectValidationWhenTokenHasAlreadyBeenUsed() {
        EventRequest request = approvedRequest();
        request.setActivationTokenUsedAt(Instant.now().minusSeconds(10));

        when(eventRequestRepositoryPort.findByActivationToken(request.getActivationToken())).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> eventRequestActivationService.validate(request.getActivationToken()))
                .isInstanceOf(InvalidActivationTokenException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    void shouldProvisionNewOrganizerWhenNoUserExists() {
        EventRequest request = approvedRequest();
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(eventRequestRepositoryPort.findByActivationToken(request.getActivationToken())).thenReturn(Optional.of(request));
        when(eventRequestAccountProvisioningPort.findUserByEmail(request.getRequesterEmail())).thenReturn(Optional.empty());
        when(eventRequestAccountProvisioningPort.provisionOrganizerAccount(any()))
                .thenReturn(new EventRequestAccountProvisioningPort.AccountProvisioningResult(userId, true, Set.of("organizer", "participant")));
        when(eventRequestRepositoryPort.save(any(EventRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivateEventRequestUseCase.ActivateResult result = eventRequestActivationService.activate(
                new ActivateEventRequestUseCase.ActivateCommand(request.getActivationToken(), "NovaSenha123", "NovaSenha123", null)
        );

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.userCreated()).isTrue();
        assertThat(result.eventId()).isEqualTo(request.getLinkedEventId());
        verify(eventService).assignOrganizer(request.getLinkedEventId(), userId);
        verify(eventRequestRepositoryPort).save(request);
    }

    @Test
    void shouldRequireAuthenticatedUserWhenAccountAlreadyExists() {
        EventRequest request = approvedRequest();
        EventRequestAccountProvisioningPort.UserAccount existingUser = new EventRequestAccountProvisioningPort.UserAccount(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                request.getRequesterEmail(),
                Set.of("participant"),
                true
        );

        when(eventRequestRepositoryPort.findByActivationToken(request.getActivationToken())).thenReturn(Optional.of(request));
        when(eventRequestAccountProvisioningPort.findUserByEmail(request.getRequesterEmail())).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> eventRequestActivationService.activate(
                new ActivateEventRequestUseCase.ActivateCommand(request.getActivationToken(), null, null, null)
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("requires authentication");
    }

    @Test
    void shouldLinkExistingAuthenticatedUserWhenAccountAlreadyExists() {
        EventRequest request = approvedRequest();
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        EventRequestAccountProvisioningPort.UserAccount existingUser = new EventRequestAccountProvisioningPort.UserAccount(
                userId,
                request.getRequesterEmail(),
                Set.of("participant"),
                true
        );

        when(eventRequestRepositoryPort.findByActivationToken(request.getActivationToken())).thenReturn(Optional.of(request));
        when(eventRequestAccountProvisioningPort.findUserByEmail(request.getRequesterEmail())).thenReturn(Optional.of(existingUser));
        when(eventRequestAccountProvisioningPort.linkApprovedRequestToExistingUser(any()))
                .thenReturn(new EventRequestAccountProvisioningPort.AccountProvisioningResult(userId, false, Set.of("organizer", "participant")));
        when(eventRequestRepositoryPort.save(any(EventRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivateEventRequestUseCase.ActivateResult result = eventRequestActivationService.activate(
                new ActivateEventRequestUseCase.ActivateCommand(request.getActivationToken(), null, null, userId)
        );

        assertThat(result.userCreated()).isFalse();
        assertThat(result.userId()).isEqualTo(userId);
        verify(eventService).assignOrganizer(request.getLinkedEventId(), userId);
    }

    @Test
    void shouldRejectWhenTrackingCombinationDoesNotExist() {
        EventRequestConfig eventRequestConfig = new EventRequestConfig();
        TrackEventRequestService trackEventRequestService = new TrackEventRequestService(eventRequestRepositoryPort, eventRequestConfig);

        when(eventRequestRepositoryPort.findByTrackingCodeAndRequesterEmail("SOL-2026-XXXX", "missing@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> trackEventRequestService.track(
                new TrackEventRequestUseCase.TrackCommand("SOL-2026-XXXX", "missing@example.com")
        )).isInstanceOf(ResourceNotFoundException.class);
    }

    private EventRequest approvedRequest() {
        EventRequest request = EventRequest.builder()
                .trackingCode("SOL-2026-A3F8")
                .status(EventRequestStatus.APPROVED)
                .requesterFirstName("Ana")
                .requesterLastName("Silva")
                .requesterName("Ana Silva")
                .requesterEmail("ana.silva@example.com")
                .requesterPhone("+5511999999999")
                .eventName("Congresso Brasileiro de IA")
                .eventTheme("Aplicações de IA na pesquisa científica")
                .eventStartDate(LocalDate.of(2026, 9, 10))
                .eventEndDate(LocalDate.of(2026, 9, 12))
                .eventLocation("Florianópolis")
                .eventType("conference")
                .hasRegistration(true)
                .hasSubmission(true)
                .hasSite(true)
                .activationToken(UUID.fromString("11111111-1111-1111-1111-111111111111"))
                .activationTokenExpiresAt(Instant.now().plusSeconds(3600))
                .linkedEventId(UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .build();
        request.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        request.addHistoryEntry(EventRequestStatus.APPROVED, "Approved by admin", UUID.fromString("55555555-5555-5555-5555-555555555555"));
        setEntityField(request, "id", UUID.fromString("22222222-2222-2222-2222-222222222222"));
        setEntityField(request, "createdAt", Instant.parse("2026-04-01T10:15:30Z"));
        request.getStatusHistory().forEach(entry -> setEntityField(entry, "createdAt", Instant.parse("2026-04-01T10:15:30Z")));
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