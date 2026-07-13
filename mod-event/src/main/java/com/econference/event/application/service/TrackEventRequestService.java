package com.econference.event.application.service;

import com.econference.event.application.port.in.TrackEventRequestUseCase;
import com.econference.event.application.port.out.EventRequestRepositoryPort;
import com.econference.event.config.EventRequestConfig;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackEventRequestService implements TrackEventRequestUseCase {

    private final EventRequestRepositoryPort eventRequestRepositoryPort;
    private final EventRequestConfig eventRequestConfig;

    @Override
    public TrackResult track(TrackCommand command) {
        EventRequest eventRequest = eventRequestRepositoryPort.findByTrackingCodeAndRequesterEmail(
                        command.trackingCode(),
                        command.email()
                )
                .orElseThrow(() -> new ResourceNotFoundException("Event request", "tracking details"));

        return new TrackResult(
                eventRequest.getTrackingCode(),
                normalizeStatus(eventRequest.getStatus()),
                eventRequest.getEventName(),
                eventRequest.getStatusHistory().stream()
                        .map(entry -> new HistoryItem(
                                normalizeStatus(entry.getStatus()),
                                entry.getCreatedAt(),
                                entry.getMessage()
                        ))
                        .toList(),
                resolveActivationUrl(eventRequest),
                eventRequest.getCreatedAt()
        );
    }

    private String resolveActivationUrl(EventRequest eventRequest) {
        if (eventRequest.getStatus() != EventRequestStatus.APPROVED) {
            return null;
        }
        if (eventRequest.getActivationToken() == null || eventRequest.getActivationTokenUsedAt() != null) {
            return null;
        }
        if (eventRequest.getActivationTokenExpiresAt() != null && eventRequest.getActivationTokenExpiresAt().isBefore(Instant.now())) {
            return null;
        }

        String baseUrl = trimTrailingSlash(eventRequestConfig.getBaseUrl());
        return baseUrl + eventRequestConfig.getActivationPath() + "?token=" + eventRequest.getActivationToken();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String normalizeStatus(Enum<?> status) {
        return status.name().toLowerCase();
    }
}