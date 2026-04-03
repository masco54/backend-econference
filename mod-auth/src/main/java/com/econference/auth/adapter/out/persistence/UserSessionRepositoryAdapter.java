package com.econference.auth.adapter.out.persistence;

import com.econference.auth.application.port.out.UserSessionRepositoryPort;
import com.econference.auth.domain.UserSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserSessionRepositoryAdapter implements UserSessionRepositoryPort {

    private final UserSessionJpaRepository jpaRepository;

    @Override
    public UserSession save(UserSession session) {
        return jpaRepository.save(session);
    }

    @Override
    public Optional<UserSession> findActiveByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHashAndInvalidatedAtIsNull(tokenHash);
    }

    @Override
    @Transactional
    public int invalidateAllByUserId(UUID userId) {
        return jpaRepository.invalidateAllByUserId(userId, Instant.now());
    }
}
