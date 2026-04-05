package com.econference.event.application.port.in;

import com.econference.event.domain.EventRequest;

import java.util.UUID;

public interface AddEventRequestNoteUseCase {

    EventRequest addNote(AddNoteCommand command);

    record AddNoteCommand(UUID requestId, String note, UUID adminUserId) {
    }
}