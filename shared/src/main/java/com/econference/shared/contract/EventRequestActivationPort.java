package com.econference.shared.contract;

import java.util.UUID;

/**
 * Cross-module port for delegating organizer activation operations to mod-event.
 * mod-auth uses this port; mod-event implements it.
 */
public interface EventRequestActivationPort {

    ValidateActivationResult validateActivation(UUID token);

    CompleteActivationResult completeActivation(CompleteActivationCommand command);

    record ValidateActivationResult(
            boolean valid,
            String reason,
            String firstName,
            String lastName,
            String email,
            String phone,
            String eventName
    ) {}

    record CompleteActivationCommand(UUID token, String password, String phone) {}

    record CompleteActivationResult(UUID userId, UUID eventId) {}
}
