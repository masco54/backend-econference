package com.econference.event.adapter.in.web.dto.response;

import java.time.Instant;
import java.util.List;

public record TrackEventRequestResponse(
        String trackingCode,
        String status,
        String eventName,
        List<StatusHistoryEntryResponse> statusHistory,
        String activationUrl,
        Instant createdAt
) {

    public record StatusHistoryEntryResponse(
            String status,
            Instant timestamp,
            String message
    ) {
    }
}