package com.econference.event.application.service;

import com.econference.event.application.exception.DuplicateEventRequestException;
import com.econference.event.application.exception.InvalidEventRequestStateException;
import com.econference.event.application.port.in.SubmitEventRequestUseCase;
import com.econference.event.application.port.out.EventRequestRepositoryPort;
import com.econference.event.application.port.out.TrackingCodeGeneratorPort;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SubmitEventRequestService implements SubmitEventRequestUseCase {

    private static final String SUBMISSION_MESSAGE = "Solicitação registrada com sucesso.";

    private final EventRequestRepositoryPort eventRequestRepositoryPort;
    private final TrackingCodeGeneratorPort trackingCodeGeneratorPort;

    @Override
    @Transactional
    public SubmitResult submit(SubmitCommand command) {
        if (command.eventEndDate().isBefore(command.eventStartDate())) {
            throw new InvalidEventRequestStateException("Event end date must be on or after the start date");
        }

        if (eventRequestRepositoryPort.existsOpenRequest(command.requesterEmail(), command.eventName())) {
            throw new DuplicateEventRequestException(command.requesterEmail(), command.eventName());
        }

        EventRequest eventRequest = EventRequest.builder()
                .trackingCode(trackingCodeGeneratorPort.generateTrackingCode())
                .status(EventRequestStatus.REQUESTED)
                .requesterFirstName(command.requesterFirstName())
                .requesterLastName(command.requesterLastName())
                .requesterName(resolveRequesterName(command))
                .requesterEmail(command.requesterEmail())
                .requesterPhone(command.requesterPhone())
                .requesterInstitution(command.requesterInstitution())
                .eventName(command.eventName())
                .eventTheme(command.eventTheme())
                .eventStartDate(command.eventStartDate())
                .eventEndDate(command.eventEndDate())
                .eventLocation(command.eventLocation())
                .eventType(command.eventType())
                .hasRegistration(command.hasRegistration())
                .hasSubmission(command.hasSubmission())
                .hasSite(command.hasSite())
                .build();

        eventRequest.addHistoryEntry(EventRequestStatus.REQUESTED, "Event request submitted", null);
        EventRequest saved = eventRequestRepositoryPort.save(eventRequest);

        return new SubmitResult(saved.getId(), saved.getTrackingCode(), SUBMISSION_MESSAGE);
    }

    private String resolveRequesterName(SubmitCommand command) {
        if (command.requesterName() != null && !command.requesterName().isBlank()) {
            return command.requesterName().trim();
        }
        return (command.requesterFirstName().trim() + " " + command.requesterLastName().trim()).trim();
    }
}