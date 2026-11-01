package com.econference.event.domain;

import com.econference.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 20)
    private String acronym;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    private String location;

    @Column(name = "organizer_id", nullable = false)
    private UUID organizerId;

    @Column(length = 50)
    private String type;

    @Column(columnDefinition = "TEXT")
    private String theme;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "has_registration", nullable = false)
    @Builder.Default
    private boolean hasRegistration = false;

    @Column(name = "has_submission", nullable = false)
    @Builder.Default
    private boolean hasSubmission = false;

    @Column(name = "has_site", nullable = false)
    @Builder.Default
    private boolean hasSite = false;

    @Column(name = "configuration_completed", nullable = false)
    @Builder.Default
    private boolean configurationCompleted = false;

    @Column(name = "registration_start_date")
    private LocalDate registrationStartDate;

    @Column(name = "registration_end_date")
    private LocalDate registrationEndDate;

    @Column(name = "submission_start_date")
    private LocalDate submissionStartDate;

    @Column(name = "submission_end_date")
    private LocalDate submissionEndDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EventStatus status = EventStatus.APPROVED;

    public static Event draftOperational(String name,
                                           String description,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           String location,
                                           UUID organizerId) {
        return Event.builder()
                .name(name)
                .description(description)
                .startDate(startDate)
                .endDate(endDate)
                .location(location)
                .organizerId(organizerId)
                .hasRegistration(false)
                .hasSubmission(false)
                .hasSite(false)
                .configurationCompleted(false)
                .status(EventStatus.APPROVED)
                .build();
    }

    public void assignOrganizer(UUID organizerId) {
        this.organizerId = organizerId;
    }
}
