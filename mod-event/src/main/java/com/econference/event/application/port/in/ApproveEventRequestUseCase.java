package com.econference.event.application.port.in;

import com.econference.event.domain.EventRequest;

import java.util.UUID;

public interface ApproveEventRequestUseCase {

    EventRequest approve(ApproveCommand command);

    record ApproveCommand(UUID requestId, String adminNotes, UUID adminUserId) {
    }
}