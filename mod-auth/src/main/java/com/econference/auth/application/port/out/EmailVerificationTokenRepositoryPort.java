package com.econference.auth.application.port.out;

import com.econference.auth.domain.EmailVerificationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepositoryPort {

    EmailVerificationToken save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByToken(String token);

    List<EmailVerificationToken> findActiveByUserId(UUID userId);
}
