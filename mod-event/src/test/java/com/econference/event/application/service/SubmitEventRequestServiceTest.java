package com.econference.event.application.service;

import com.econference.event.application.exception.DuplicateEventRequestException;
import com.econference.event.application.exception.InvalidEventRequestStateException;
import com.econference.event.application.port.in.SubmitEventRequestUseCase;
import com.econference.event.application.port.out.EventRequestRepositoryPort;
import com.econference.event.application.port.out.TrackingCodeGeneratorPort;
import com.econference.event.domain.EventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitEventRequestServiceTest {

    @Mock
    private EventRequestRepositoryPort eventRequestRepositoryPort;

    @Mock
    private TrackingCodeGeneratorPort trackingCodeGeneratorPort;

    @InjectMocks
    private SubmitEventRequestService submitEventRequestService;

    @Test
    void shouldRejectWhenEventEndDateIsBeforeStartDate() {
        SubmitEventRequestUseCase.SubmitCommand command = validCommand(
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 10)
        );

        assertThatThrownBy(() -> submitEventRequestService.submit(command))
                .isInstanceOf(InvalidEventRequestStateException.class)
                .hasMessageContaining("Event end date");
    }

    @Test
    void shouldRejectWhenOpenRequestAlreadyExists() {
        SubmitEventRequestUseCase.SubmitCommand command = validCommand(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12)
        );

        when(eventRequestRepositoryPort.existsOpenRequest(command.requesterEmail(), command.eventName())).thenReturn(true);

        assertThatThrownBy(() -> submitEventRequestService.submit(command))
                .isInstanceOf(DuplicateEventRequestException.class)
                .hasMessageContaining(command.requesterEmail());
    }

    @Test
    void shouldCreateRequestWithTrackingCodeAndInitialHistoryEntry() {
        SubmitEventRequestUseCase.SubmitCommand command = validCommand(
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 12)
        );

        when(eventRequestRepositoryPort.existsOpenRequest(command.requesterEmail(), command.eventName())).thenReturn(false);
        when(trackingCodeGeneratorPort.generateTrackingCode()).thenReturn("SOL-2026-A3F8");
        when(eventRequestRepositoryPort.save(any(EventRequest.class))).thenAnswer(invocation -> {
            EventRequest eventRequest = invocation.getArgument(0);
            setEntityId(eventRequest, UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return eventRequest;
        });

        SubmitEventRequestUseCase.SubmitResult result = submitEventRequestService.submit(command);

        assertThat(result.id()).isEqualTo(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        assertThat(result.trackingCode()).isEqualTo("SOL-2026-A3F8");
        assertThat(result.message()).isEqualTo("Solicitação registrada com sucesso.");
        verify(eventRequestRepositoryPort).save(any(EventRequest.class));
    }

    private SubmitEventRequestUseCase.SubmitCommand validCommand(LocalDate startDate, LocalDate endDate) {
        return new SubmitEventRequestUseCase.SubmitCommand(
                "Ana",
                "Silva",
                null,
                "ana.silva@example.com",
                "+5511999999999",
                "UFSC",
                "Congresso Brasileiro de IA",
                "Aplicações de IA na pesquisa científica",
                startDate,
                endDate,
                "Florianópolis",
                "conference",
                true,
                true,
                true
        );
    }

    private void setEntityId(EventRequest eventRequest, UUID id) {
        try {
            Class<?> entityType = eventRequest.getClass().getSuperclass();
            var field = entityType.getDeclaredField("id");
            field.setAccessible(true);
            field.set(eventRequest, id);
        } catch (ReflectiveOperationException ex) {
            throw new AssertionError(ex);
        }
    }
}