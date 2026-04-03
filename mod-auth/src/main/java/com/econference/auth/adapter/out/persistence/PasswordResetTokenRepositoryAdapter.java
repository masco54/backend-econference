package com.econference.auth.adapter.out.persistence;

import com.econference.auth.application.port.out.PasswordResetTokenRepositoryPort;
import com.econference.auth.domain.PasswordResetToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepositoryPort {

    private final PasswordResetTokenJpaRepository jpaRepository;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<PasswordResetToken> findByToken(String token) {
        return jpaRepository.findByToken(token);
    }

    @Override
    public List<PasswordResetToken> findActiveByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndUsedAtIsNull(userId);
    }
}
