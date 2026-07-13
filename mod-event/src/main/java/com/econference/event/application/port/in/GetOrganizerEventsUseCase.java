package com.econference.event.application.port.in;

import com.econference.event.domain.Event;

import java.util.List;
import java.util.UUID;

public interface GetOrganizerEventsUseCase {

    OrganizerListResult get(OrganizerListCommand command);

    record OrganizerListCommand(UUID organizerId, String status, int page, int limit) {
    }

    record OrganizerListResult(List<Event> data, Pagination pagination) {
    }

    record Pagination(int page, int limit, long total, int totalPages) {
    }
}
