package com.econference.event.application.port.in;

import java.time.Instant;
import java.util.List;

public interface TrackEventRequestUseCase {

    TrackResult track(TrackCommand command);

    record TrackCommand(String trackingCode, String email) {
    }

    record TrackResult(
            String trackingCode,
            String status,
            String eventName,
            List<HistoryItem> statusHistory,
            String activationUrl,
            Instant createdAt
    ) {
    }

    record HistoryItem(String status, Instant timestamp, String message) {
    }
}