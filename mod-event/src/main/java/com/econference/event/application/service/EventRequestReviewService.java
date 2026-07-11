package com.econference.event.application.service;

import com.econference.event.application.exception.InvalidEventRequestStateException;
import com.econference.event.application.port.in.AddEventRequestNoteUseCase;
import com.econference.event.application.port.in.ApproveEventRequestUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestDetailUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestsUseCase;
import com.econference.event.application.port.in.ReissueActivationTokenUseCase;
import com.econference.event.application.port.in.RejectEventRequestUseCase;
import com.econference.event.application.port.out.EventRequestNotificationPort;
import com.econference.event.application.port.out.EventRequestRepositoryPort;
import com.econference.event.config.EventRequestConfig;
import com.econference.event.domain.Event;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import com.econference.event.service.EventService;
import com.econference.shared.exception.BusinessException;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRequestReviewService implements GetAdminEventRequestsUseCase,
        GetAdminEventRequestDetailUseCase,
        AddEventRequestNoteUseCase,
        ApproveEventRequestUseCase,
        RejectEventRequestUseCase,
        ReissueActivationTokenUseCase {

    private static final EnumSet<EventRequestStatus> DECISION_ELIGIBLE_STATUSES =
            EnumSet.of(EventRequestStatus.REQUESTED, EventRequestStatus.UNDER_REVIEW);

    private final EventRequestRepositoryPort eventRequestRepositoryPort;
    private final EventRequestNotificationPort eventRequestNotificationPort;
    private final EventService eventService;
    private final EventRequestConfig eventRequestConfig;

    @Override
    public AdminListResult get(AdminListCommand command) {
        int pageNumber = Math.max(command.page(), 1);
        int limit = Math.min(Math.max(command.limit(), 1), 100);
        Page<EventRequest> page = eventRequestRepositoryPort.searchAdmin(
                parseStatus(command.status()),
                command.search(),
                PageRequest.of(pageNumber - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        return new AdminListResult(
                page.getContent(),
                new Pagination(pageNumber, limit, page.getTotalElements(), page.getTotalPages())
        );
    }

    @Override
    public EventRequest get(UUID requestId) {
        return loadDetailedRequest(requestId);
    }

    @Override
    @Transactional
    public EventRequest addNote(AddNoteCommand command) {
        if (command.note() == null || command.note().isBlank()) {
            throw new BusinessException("Administrative note is required");
        }

        EventRequest request = loadDetailedRequest(command.requestId());
        ensureNoteAllowed(request);

        EventRequestStatus nextStatus = request.getStatus() == EventRequestStatus.REQUESTED
                ? EventRequestStatus.UNDER_REVIEW
                : EventRequestStatus.UNDER_REVIEW;

        request.setStatus(nextStatus);
        request.setAdminNotes(command.note().trim());
        request.addHistoryEntry(nextStatus, command.note().trim(), command.adminUserId());

        EventRequest saved = eventRequestRepositoryPort.save(request);
        eventRequestNotificationPort.sendNoteAdded(saved, command.note().trim());
        return saved;
    }

    @Override
    @Transactional
    public EventRequest approve(ApproveCommand command) {
        EventRequest request = loadDetailedRequest(command.requestId());
        ensureDecisionAllowed(request, "approved");

        Event createdEvent = eventService.createPendingOperationalEvent(
                request.getEventName(),
                request.getEventTheme(),
                request.getEventStartDate(),
                request.getEventEndDate(),
                request.getEventLocation(),
                command.adminUserId()
        );

        request.setStatus(EventRequestStatus.APPROVED);
        request.setAdminNotes(trimToNull(command.adminNotes()));
        request.setRejectionReason(null);
        request.setLinkedEventId(createdEvent.getId());
        request.setActivationToken(UUID.randomUUID());
        request.setActivationTokenExpiresAt(Instant.now().plusSeconds((long) eventRequestConfig.getActivationExpiryHours() * 3600));
        request.setActivationTokenUsedAt(null);
        request.addHistoryEntry(
                EventRequestStatus.APPROVED,
                defaultIfBlank(command.adminNotes(), "Approved by admin"),
                command.adminUserId()
        );

        EventRequest saved = eventRequestRepositoryPort.save(request);
        eventRequestNotificationPort.sendApproval(saved, buildActivationUrl(saved));
        return saved;
    }

    @Override
    @Transactional
    public EventRequest reject(RejectCommand command) {
        if (command.reason() == null || command.reason().isBlank()) {
            throw new BusinessException("Rejection reason is required");
        }

        EventRequest request = loadDetailedRequest(command.requestId());
        ensureDecisionAllowed(request, "rejected");

        request.setStatus(EventRequestStatus.REJECTED);
        request.setRejectionReason(command.reason().trim());
        request.setAdminNotes(null);
        request.addHistoryEntry(EventRequestStatus.REJECTED, command.reason().trim(), command.adminUserId());

        EventRequest saved = eventRequestRepositoryPort.save(request);
        eventRequestNotificationPort.sendRejection(saved);
        return saved;
    }

    @Override
    @Transactional
    public EventRequest reissue(ReissueCommand command) {
        EventRequest request = loadDetailedRequest(command.requestId());

        if (request.getStatus() != EventRequestStatus.APPROVED || !isTokenEligibleForReissue(request)) {
            throw new InvalidEventRequestStateException("Activation token cannot be reissued for the current request state");
        }

        request.setActivationToken(UUID.randomUUID());
        request.setActivationTokenExpiresAt(Instant.now().plusSeconds((long) eventRequestConfig.getActivationExpiryHours() * 3600));
        request.setActivationTokenUsedAt(null);
        request.addHistoryEntry(EventRequestStatus.APPROVED, "Activation token reissued", command.adminUserId());

        EventRequest saved = eventRequestRepositoryPort.save(request);
        eventRequestNotificationPort.sendActivationTokenReissued(saved, buildActivationUrl(saved));
        return saved;
    }

    private EventRequest loadDetailedRequest(UUID requestId) {
        return eventRequestRepositoryPort.findDetailedById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Event request", requestId));
    }

    private void ensureNoteAllowed(EventRequest request) {
        if (!DECISION_ELIGIBLE_STATUSES.contains(request.getStatus())) {
            throw new InvalidEventRequestStateException("Event request cannot receive administrative notes in the current state");
        }
    }

    private void ensureDecisionAllowed(EventRequest request, String action) {
        if (!DECISION_ELIGIBLE_STATUSES.contains(request.getStatus())) {
            throw new InvalidEventRequestStateException("Event request cannot be %s in the current state".formatted(action));
        }
    }

    private boolean isTokenEligibleForReissue(EventRequest request) {
        if (request.getLinkedUserId() != null) {
            return false;
        }
        if (request.getActivationToken() == null) {
            return true;
        }
        if (request.getActivationTokenUsedAt() != null) {
            return true;
        }
        return request.getActivationTokenExpiresAt() != null && request.getActivationTokenExpiresAt().isBefore(Instant.now());
    }

    private EventRequestStatus parseStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.isBlank()) {
            return null;
        }
        try {
            return EventRequestStatus.valueOf(rawStatus.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Unsupported event request status filter: " + rawStatus);
        }
    }

    private String buildActivationUrl(EventRequest request) {
        String baseUrl = trimTrailingSlash(eventRequestConfig.getBaseUrl());
        return baseUrl + "/register?token=" + request.getActivationToken();
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String defaultIfBlank(String value, String fallback) {
        String trimmed = trimToNull(value);
        return trimmed == null ? fallback : trimmed;
    }
}