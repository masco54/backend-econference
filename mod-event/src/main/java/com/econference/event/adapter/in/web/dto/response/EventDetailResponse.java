package com.econference.event.adapter.in.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record EventDetailResponse(
        UUID id,
        String name,
        String acronym,
        String description,
        String status,
        String type,
        String theme,
        String logoUrl,
        LocalDate startDate,
        LocalDate endDate,
        String location,
        UUID organizerId,
        boolean hasRegistration,
        boolean hasSubmission,
        boolean hasSite,
        boolean configurationCompleted,
        LocalDate registrationStartDate,
        LocalDate registrationEndDate,
        LocalDate submissionStartDate,
        LocalDate submissionEndDate,
        Instant createdAt
) {
}
