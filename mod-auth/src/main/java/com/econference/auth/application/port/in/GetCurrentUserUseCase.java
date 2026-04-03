package com.econference.auth.application.port.in;

import com.econference.auth.domain.User;

import java.util.UUID;

public interface GetCurrentUserUseCase {

    User getUser(UUID userId);
}
