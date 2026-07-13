package com.econference.shared.contract;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EventRequestAccountProvisioningPort {

    Optional<UserAccount> findUserByEmail(String email);

    AccountProvisioningResult provisionOrganizerAccount(ProvisionOrganizerAccountCommand command);

    AccountProvisioningResult linkApprovedRequestToExistingUser(LinkApprovedRequestToExistingUserCommand command);

    record UserAccount(UUID userId, String email, Set<String> roles, boolean emailVerified) {
    }

    record ProvisionOrganizerAccountCommand(
            String firstName,
            String lastName,
            String email,
            String phone,
            String password
    ) {
    }

    record LinkApprovedRequestToExistingUserCommand(UUID userId, UUID eventId, String expectedEmail) {
    }

    record AccountProvisioningResult(UUID userId, boolean userCreated, Set<String> roles) {
    }
}