package com.econference.event.application.port.in;

import java.util.UUID;

public interface ActivateEventRequestUseCase {

    ActivateResult activate(ActivateCommand command);

    record ActivateCommand(UUID token, String password, String confirmPassword, UUID authenticatedUserId) {
    }

    record ActivateResult(UUID eventId, UUID userId, String message, boolean userCreated) {
    }
}