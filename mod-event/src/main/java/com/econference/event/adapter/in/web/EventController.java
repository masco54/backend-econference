package com.econference.event.adapter.in.web;

import com.econference.event.adapter.in.web.dto.response.EventSummaryResponse;
import com.econference.event.adapter.in.web.dto.response.OrganizerEventsResponse;
import com.econference.event.application.port.in.GetOrganizerEventsUseCase;
import com.econference.event.domain.Event;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

    private EventSummaryResponse toResponse(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getName(),
                mapStatus(event.getStatus()),
                event.getStartDate(),
                event.getEndDate(),
                event.getLocation(),
                event.getCreatedAt()
        );
    }

    private String mapStatus(com.econference.event.domain.EventStatus status) {
        if (status == null) return null;
        return status.name().toLowerCase();
    }
}
