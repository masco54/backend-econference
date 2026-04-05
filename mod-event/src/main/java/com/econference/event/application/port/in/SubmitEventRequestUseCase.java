package com.econference.event.application.port.in;

import java.time.LocalDate;
import java.util.UUID;

public interface SubmitEventRequestUseCase {

    SubmitResult submit(SubmitCommand command);

    record SubmitCommand(
            String requesterFirstName,
            String requesterLastName,
            String requesterName,
            String requesterEmail,
            String requesterPhone,
            String requesterInstitution,
            String eventName,
            String eventTheme,
            LocalDate eventStartDate,
            LocalDate eventEndDate,
            String eventLocation,
            String eventType,
            boolean hasRegistration,
            boolean hasSubmission,
            boolean hasSite
    ) {
    }

    record SubmitResult(UUID id, String trackingCode, String message) {
    }
}