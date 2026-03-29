package com.econference.event.service;

import com.econference.event.domain.Event;
import com.econference.event.repository.EventRepository;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public Event create(Event event) {
        return eventRepository.save(event);
    }
}
