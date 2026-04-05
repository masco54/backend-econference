package com.econference.event.application.port.in;

import java.util.UUID;

public interface ValidateActivationTokenUseCase {

    ValidateResult validate(UUID token);

    record ValidateResult(boolean valid, String email, String eventName, UUID eventRequestId, boolean userExists) {
    }
}