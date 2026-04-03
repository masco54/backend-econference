package com.econference.auth.adapter.out.persistence;

import com.econference.auth.application.port.out.EmailVerificationTokenRepositoryPort;
import com.econference.auth.domain.EmailVerificationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepositoryPort {

    private final EmailVerificationTokenJpaRepository jpaRepository;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return jpaRepository.save(token);
    }

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return jpaRepository.findByToken(token);
    }

    @Override
    public List<EmailVerificationToken> findActiveByUserId(UUID userId) {
        return jpaRepository.findByUserIdAndUsedAtIsNull(userId);
    }
}
