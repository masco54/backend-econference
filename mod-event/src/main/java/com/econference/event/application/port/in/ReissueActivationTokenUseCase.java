package com.econference.event.application.port.in;

import com.econference.event.domain.EventRequest;

import java.util.UUID;

public interface ReissueActivationTokenUseCase {

    EventRequest reissue(ReissueCommand command);

    record ReissueCommand(UUID requestId, UUID adminUserId) {
    }
}