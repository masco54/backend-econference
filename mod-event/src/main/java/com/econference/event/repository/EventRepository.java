package com.econference.event.repository;

import com.econference.event.domain.Event;
import com.econference.event.domain.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    List<Event> findByOrganizerId(UUID organizerId);

    Page<Event> findByOrganizerId(UUID organizerId, Pageable pageable);

    Page<Event> findByOrganizerIdAndStatus(UUID organizerId, EventStatus status, Pageable pageable);
}
