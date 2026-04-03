package com.econference.auth.application.port.out;

import com.econference.auth.domain.PasswordResetToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepositoryPort {

    PasswordResetToken save(PasswordResetToken token);

    Optional<PasswordResetToken> findByToken(String token);

    List<PasswordResetToken> findActiveByUserId(UUID userId);
}
