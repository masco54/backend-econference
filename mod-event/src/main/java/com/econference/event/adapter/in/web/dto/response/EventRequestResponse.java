package com.econference.event.adapter.in.web.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record EventRequestResponse(
        UUID id,
        String trackingCode,
        String status,
        String requesterFirstName,
        String requesterLastName,
        String requesterName,
        String requesterEmail,
        String requesterPhone,
        String requesterInstitution,
        String eventName,
        String eventTheme,
        LocalDate eventStartDate,
        LocalDate eventEndDate,
        String eventLocation,
        String eventType,
        boolean hasRegistration,
        boolean hasSubmission,
        boolean hasSite,
        String adminNotes,
        String rejectionReason,
        UUID activationToken,
        Instant activationTokenExpiresAt,
        boolean activationTokenUsed,
        UUID linkedEventId,
        UUID linkedUserId,
        List<StatusHistoryEntryResponse> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {

    public record StatusHistoryEntryResponse(
            String status,
            Instant timestamp,
            String message
    ) {
    }
}