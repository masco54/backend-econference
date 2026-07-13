package com.econference.event.domain;

import com.econference.shared.domain.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "event_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest extends BaseEntity {

    @Column(name = "tracking_code", nullable = false, unique = true, length = 20)
    private String trackingCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private EventRequestStatus status = EventRequestStatus.REQUESTED;

    @Column(name = "requester_first_name", nullable = false, length = 50)
    private String requesterFirstName;

    @Column(name = "requester_last_name", nullable = false, length = 50)
    private String requesterLastName;

    @Column(name = "requester_name", nullable = false, length = 100)
    private String requesterName;

    @Column(name = "requester_email", nullable = false, length = 255)
    private String requesterEmail;

    @Column(name = "requester_phone", length = 20)
    private String requesterPhone;

    @Column(name = "requester_institution", length = 255)
    private String requesterInstitution;

    @Column(name = "event_name", nullable = false, length = 200)
    private String eventName;

    @Column(name = "event_theme", nullable = false, length = 500)
    private String eventTheme;

    @Column(name = "event_start_date", nullable = false)
    private LocalDate eventStartDate;

    @Column(name = "event_end_date", nullable = false)
    private LocalDate eventEndDate;

    @Column(name = "event_location", nullable = false, length = 200)
    private String eventLocation;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "has_registration", nullable = false)
    private boolean hasRegistration;

    @Column(name = "has_submission", nullable = false)
    private boolean hasSubmission;

    @Column(name = "has_site", nullable = false)
    private boolean hasSite;

    @Column(name = "admin_notes", length = 1000)
    private String adminNotes;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "activation_token")
    private UUID activationToken;

    @Column(name = "activation_token_expires_at")
    private Instant activationTokenExpiresAt;

    @Column(name = "activation_token_used_at")
    private Instant activationTokenUsedAt;

    @Column(name = "linked_event_id")
    private UUID linkedEventId;

    @Column(name = "linked_user_id")
    private UUID linkedUserId;

    @OneToMany(mappedBy = "eventRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<EventRequestStatusHistory> statusHistory = new ArrayList<>();

    public void addHistoryEntry(EventRequestStatus nextStatus, String message, UUID actorUserId) {
        EventRequestStatusHistory entry = EventRequestStatusHistory.builder()
                .eventRequest(this)
                .status(nextStatus)
                .message(message)
                .actorUserId(actorUserId)
                .build();
        statusHistory.add(entry);
    }
}