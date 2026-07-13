package com.econference.auth.adapter.in.web.dto.response;

import java.util.Set;
import java.util.UUID;

public record CompleteOrganizerActivationResponse(
        UUID userId,
        Set<String> roles,
        UUID eventId,
        UserInfo user
) {
    public record UserInfo(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            Set<String> roles,
            boolean emailVerified
    ) {}
}
