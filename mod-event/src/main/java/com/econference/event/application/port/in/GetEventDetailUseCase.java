package com.econference.event.application.port.in;

import com.econference.event.domain.Event;

import java.util.UUID;

public interface GetEventDetailUseCase {

    EventDetailResult get(EventDetailCommand command);

    record EventDetailCommand(UUID eventId, UUID requesterId, boolean isAdmin) {
    }

    record EventDetailResult(Event data) {
    }
}
