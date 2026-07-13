package com.econference.event.adapter.in.web.dto.response;

import java.util.UUID;

public record ActivateEventRequestResponse(
        UUID eventId,
        UUID userId,
        String message,
        boolean userCreated
) {
}