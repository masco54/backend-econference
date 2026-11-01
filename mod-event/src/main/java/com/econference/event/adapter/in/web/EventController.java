package com.econference.event.adapter.in.web;

import com.econference.event.adapter.in.web.dto.response.EventDetailResponse;
import com.econference.event.adapter.in.web.dto.response.EventSummaryResponse;
import com.econference.event.adapter.in.web.dto.response.OrganizerEventsResponse;
import com.econference.event.application.port.in.GetEventDetailUseCase;
import com.econference.event.application.port.in.GetOrganizerEventsUseCase;
import com.econference.event.domain.Event;
import com.econference.shared.exception.ErrorResponse;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final GetOrganizerEventsUseCase getOrganizerEventsUseCase;
        private final GetEventDetailUseCase getEventDetailUseCase;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("module", "event", "status", "up"));
    }

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<OrganizerEventsResponse> listMyEvents(
            @AuthenticationPrincipal(expression = "userId") UUID organizerId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit
    ) {
        GetOrganizerEventsUseCase.OrganizerListResult result = getOrganizerEventsUseCase.get(
                new GetOrganizerEventsUseCase.OrganizerListCommand(organizerId, status, page, limit)
        );

        List<EventSummaryResponse> data = result.data().stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(new OrganizerEventsResponse(
                data,
                new OrganizerEventsResponse.PaginationResponse(
                        result.pagination().page(),
                        result.pagination().limit(),
                        result.pagination().total(),
                        result.pagination().totalPages()
                )
        ));
    }

    @GetMapping("/{eventId}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Map<String, EventDetailResponse>> getEventDetail(
            @PathVariable UUID eventId,
            @AuthenticationPrincipal(expression = "userId") UUID requesterId,
            Authentication authentication
    ) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));

        GetEventDetailUseCase.EventDetailResult result = getEventDetailUseCase.get(
                new GetEventDetailUseCase.EventDetailCommand(eventId, requesterId, isAdmin)
        );

        return ResponseEntity.ok(Map.of("data", toDetailResponse(result.data())));
    }

    private EventSummaryResponse toResponse(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getName(),
                mapStatus(event.getStatus()),
                                event.getType(),
                                event.getTheme(),
                                event.getLogoUrl(),
                event.getStartDate(),
                event.getEndDate(),
                event.getLocation(),
                                event.isConfigurationCompleted(),
                event.getCreatedAt()
        );
    }

    private String mapStatus(com.econference.event.domain.EventStatus status) {
        if (status == null) return null;
        return status.name().toLowerCase();
    }

        private EventDetailResponse toDetailResponse(Event event) {
                return new EventDetailResponse(
                                event.getId(),
                                event.getName(),
                                event.getAcronym(),
                                event.getDescription(),
                                mapStatus(event.getStatus()),
                                event.getType(),
                                event.getTheme(),
                                event.getLogoUrl(),
                                event.getStartDate(),
                                event.getEndDate(),
                                event.getLocation(),
                                event.getOrganizerId(),
                                event.isHasRegistration(),
                                event.isHasSubmission(),
                                event.isHasSite(),
                                event.isConfigurationCompleted(),
                                event.getRegistrationStartDate(),
                                event.getRegistrationEndDate(),
                                event.getSubmissionStartDate(),
                                event.getSubmissionEndDate(),
                                event.getCreatedAt()
                );
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleEventNotFound(ResourceNotFoundException ex) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new ErrorResponse(ex.getMessage(), "RESOURCE_NOT_FOUND"));
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(new ErrorResponse(ex.getMessage(), "FORBIDDEN"));
        }
}
