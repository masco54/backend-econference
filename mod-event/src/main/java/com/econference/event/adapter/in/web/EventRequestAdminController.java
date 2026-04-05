package com.econference.event.adapter.in.web;

import com.econference.event.adapter.in.web.dto.request.AddEventRequestNoteRequest;
import com.econference.event.adapter.in.web.dto.request.ApproveEventRequestRequest;
import com.econference.event.adapter.in.web.dto.request.RejectEventRequestRequest;
import com.econference.event.adapter.in.web.dto.response.AdminEventRequestListResponse;
import com.econference.event.adapter.in.web.dto.response.EventRequestResponse;
import com.econference.event.application.port.in.AddEventRequestNoteUseCase;
import com.econference.event.application.port.in.ApproveEventRequestUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestDetailUseCase;
import com.econference.event.application.port.in.GetAdminEventRequestsUseCase;
import com.econference.event.application.port.in.ReissueActivationTokenUseCase;
import com.econference.event.application.port.in.RejectEventRequestUseCase;
import com.econference.event.domain.EventRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/event-requests")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EventRequestAdminController {

    private final GetAdminEventRequestsUseCase getAdminEventRequestsUseCase;
    private final GetAdminEventRequestDetailUseCase getAdminEventRequestDetailUseCase;
    private final AddEventRequestNoteUseCase addEventRequestNoteUseCase;
    private final ApproveEventRequestUseCase approveEventRequestUseCase;
    private final RejectEventRequestUseCase rejectEventRequestUseCase;
    private final ReissueActivationTokenUseCase reissueActivationTokenUseCase;

    @GetMapping
    public ResponseEntity<AdminEventRequestListResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        GetAdminEventRequestsUseCase.AdminListResult result = getAdminEventRequestsUseCase.get(
                new GetAdminEventRequestsUseCase.AdminListCommand(status, search, page, limit)
        );

        List<EventRequestResponse> data = result.data().stream()
                .map(request -> toResponse(request, false))
                .toList();

        return ResponseEntity.ok(new AdminEventRequestListResponse(
                data,
                new AdminEventRequestListResponse.PaginationResponse(
                        result.pagination().page(),
                        result.pagination().limit(),
                        result.pagination().total(),
                        result.pagination().totalPages()
                )
        ));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<EventRequestResponse> detail(@PathVariable UUID requestId) {
        return ResponseEntity.ok(toResponse(getAdminEventRequestDetailUseCase.get(requestId), true));
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<EventRequestResponse> approve(
            @PathVariable UUID requestId,
            @Valid @RequestBody(required = false) ApproveEventRequestRequest request,
            @AuthenticationPrincipal(expression = "userId") UUID adminUserId
    ) {
        EventRequest approved = approveEventRequestUseCase.approve(new ApproveEventRequestUseCase.ApproveCommand(
                requestId,
                request == null ? null : request.adminNotes(),
                adminUserId
        ));
        return ResponseEntity.ok(toResponse(approved, true));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<EventRequestResponse> reject(
            @PathVariable UUID requestId,
            @Valid @RequestBody RejectEventRequestRequest request,
            @AuthenticationPrincipal(expression = "userId") UUID adminUserId
    ) {
        EventRequest rejected = rejectEventRequestUseCase.reject(new RejectEventRequestUseCase.RejectCommand(
                requestId,
                request.reason(),
                adminUserId
        ));
        return ResponseEntity.ok(toResponse(rejected, true));
    }

    @PostMapping("/{requestId}/add-note")
    public ResponseEntity<EventRequestResponse> addNote(
            @PathVariable UUID requestId,
            @Valid @RequestBody AddEventRequestNoteRequest request,
            @AuthenticationPrincipal(expression = "userId") UUID adminUserId
    ) {
        EventRequest updated = addEventRequestNoteUseCase.addNote(new AddEventRequestNoteUseCase.AddNoteCommand(
                requestId,
                request.note(),
                adminUserId
        ));
        return ResponseEntity.ok(toResponse(updated, true));
    }

    @PostMapping("/{requestId}/reissue-token")
    public ResponseEntity<EventRequestResponse> reissueToken(
            @PathVariable UUID requestId,
            @AuthenticationPrincipal(expression = "userId") UUID adminUserId
    ) {
        EventRequest updated = reissueActivationTokenUseCase.reissue(
                new ReissueActivationTokenUseCase.ReissueCommand(requestId, adminUserId)
        );
        return ResponseEntity.ok(toResponse(updated, true));
    }

    private EventRequestResponse toResponse(EventRequest request, boolean includeHistory) {
        return new EventRequestResponse(
                request.getId(),
                request.getTrackingCode(),
                normalizeStatus(request.getStatus()),
                request.getRequesterFirstName(),
                request.getRequesterLastName(),
                request.getRequesterName(),
                request.getRequesterEmail(),
                request.getRequesterPhone(),
                request.getRequesterInstitution(),
                request.getEventName(),
                request.getEventTheme(),
                request.getEventStartDate(),
                request.getEventEndDate(),
                request.getEventLocation(),
                request.getEventType(),
                request.isHasRegistration(),
                request.isHasSubmission(),
                request.isHasSite(),
                request.getAdminNotes(),
                request.getRejectionReason(),
                request.getActivationToken(),
                request.getActivationTokenExpiresAt(),
                request.getActivationTokenUsedAt() != null,
                request.getLinkedEventId(),
                request.getLinkedUserId(),
                includeHistory
                        ? request.getStatusHistory().stream()
                        .map(entry -> new EventRequestResponse.StatusHistoryEntryResponse(
                                normalizeStatus(entry.getStatus()),
                                entry.getCreatedAt(),
                                entry.getMessage()
                        ))
                        .toList()
                        : List.of(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }

    private String normalizeStatus(Enum<?> status) {
        return status.name().toLowerCase();
    }
}