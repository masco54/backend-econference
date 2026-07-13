package com.econference.event.service;

import com.econference.event.domain.Event;
import com.econference.event.domain.EventStatus;
import com.econference.event.repository.EventRepository;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;

    public Event findById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    public List<Event> findByOrganizer(UUID organizerId) {
        return eventRepository.findByOrganizerId(organizerId);
    }

    public Page<Event> findByOrganizerPaged(UUID organizerId, Pageable pageable) {
        return eventRepository.findByOrganizerId(organizerId, pageable);
    }

    public Page<Event> findByOrganizerAndStatusPaged(UUID organizerId, EventStatus status, Pageable pageable) {
        return eventRepository.findByOrganizerIdAndStatus(organizerId, status, pageable);
    }

    @Transactional
    public Event create(Event event) {
        return eventRepository.save(event);
    }

    @Transactional
    public Event createPendingOperationalEvent(String name,
                                               String description,
                                               LocalDate startDate,
                                               LocalDate endDate,
                                               String location,
                                               UUID organizerId) {
        return eventRepository.save(Event.pendingOperational(name, description, startDate, endDate, location, organizerId));
    }

    @Transactional
    public Event assignOrganizer(UUID eventId, UUID organizerId) {
        Event event = findById(eventId);
        event.assignOrganizer(organizerId);
        return eventRepository.save(event);
    }
}
