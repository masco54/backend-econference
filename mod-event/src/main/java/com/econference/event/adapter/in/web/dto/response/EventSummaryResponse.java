package com.econference.event.adapter.in.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventSummaryResponse(
        UUID id,
        String name,
        String status,
        String type,
        String theme,
        String logoUrl,
        LocalDate startDate,
        LocalDate endDate,
        String location,
        boolean configurationCompleted,
        Instant createdAt
) {
}
