package com.econference.auth.application.port.in;

import java.util.Set;
import java.util.UUID;

public interface CompleteOrganizerActivationUseCase {

    ActivationResult complete(CompleteOrganizerActivationCommand command);

    record CompleteOrganizerActivationCommand(String token, String password, String phone) {}

    record ActivationResult(UUID userId, Set<String> roles, UUID eventId,
                            String firstName, String lastName, String email,
                            String phone, boolean emailVerified) {}
}
