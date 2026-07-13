package com.econference.event.adapter.in.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventSummaryResponse(
        UUID id,
        String name,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String location,
        Instant createdAt
) {
}
