package com.econference.auth.application.port.out;

import com.econference.auth.domain.UserSession;

import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepositoryPort {

    UserSession save(UserSession session);

    Optional<UserSession> findActiveByTokenHash(String tokenHash);

    int invalidateAllByUserId(UUID userId);
}
