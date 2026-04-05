package com.econference.event.application.port.in;

import com.econference.event.domain.EventRequest;

import java.util.UUID;

public interface RejectEventRequestUseCase {

    EventRequest reject(RejectCommand command);

    record RejectCommand(UUID requestId, String reason, UUID adminUserId) {
    }
}