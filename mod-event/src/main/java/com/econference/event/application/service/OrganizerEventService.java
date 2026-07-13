package com.econference.event.application.service;

import com.econference.event.application.port.in.GetOrganizerEventsUseCase;
import com.econference.event.domain.Event;
import com.econference.event.domain.EventStatus;
import com.econference.event.service.EventService;
import com.econference.shared.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizerEventService implements GetOrganizerEventsUseCase {

    private final EventService eventService;

    @Override
    public OrganizerListResult get(OrganizerListCommand command) {
        int page = Math.max(command.page(), 1);
        int limit = Math.min(Math.max(command.limit(), 1), 100);
        UUID organizerId = command.organizerId();

        PageRequest pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Event> result;
        if (command.status() != null && !command.status().isBlank()) {
            EventStatus status = parseStatus(command.status());
            result = eventService.findByOrganizerAndStatusPaged(organizerId, status, pageable);
        } else {
            result = eventService.findByOrganizerPaged(organizerId, pageable);
        }

        return new OrganizerListResult(
                result.getContent(),
                new Pagination(page, limit, result.getTotalElements(), result.getTotalPages())
        );
    }

    private EventStatus parseStatus(String statusStr) {
        try {
            return EventStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.econference.shared.exception.BusinessException("Invalid status value: '" + statusStr + "'");
        }
    }
}
