package com.econference.event.adapter.in.web.dto.response;

import java.util.UUID;

public record SubmitEventRequestResponse(
        UUID id,
        String trackingCode,
        String message
) {
}