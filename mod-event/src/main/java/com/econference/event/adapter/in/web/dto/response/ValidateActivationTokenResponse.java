package com.econference.event.adapter.in.web.dto.response;

import java.util.UUID;

public record ValidateActivationTokenResponse(
        boolean valid,
        String email,
        String eventName,
        UUID eventRequestId,
        boolean userExists
) {
}