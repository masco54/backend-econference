package com.econference.event.application.service;

import com.econference.event.application.port.in.GetEventDetailUseCase;
import com.econference.event.domain.Event;
import com.econference.event.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerEventDetailService implements GetEventDetailUseCase {

    private final EventService eventService;
    private final OrganizerEventService organizerEventService;

    @Override
    public EventDetailResult get(EventDetailCommand command) {
        Event event = eventService.findById(command.eventId());

        if (!organizerEventService.canAccessEvent(event, command.requesterId(), command.isAdmin())) {
            throw new AccessDeniedException("You do not have permission to access this event");
        }

        return new EventDetailResult(event);
    }
}
