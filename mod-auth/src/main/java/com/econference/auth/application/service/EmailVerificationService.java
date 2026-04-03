package com.econference.auth.application.service;

import com.econference.auth.application.exception.InvalidTokenException;
import com.econference.auth.application.port.in.ResendVerificationUseCase;
import com.econference.auth.application.port.in.VerifyEmailUseCase;
import com.econference.auth.application.port.out.EmailSenderPort;
import com.econference.auth.application.port.out.EmailVerificationTokenRepositoryPort;
import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.config.EmailConfig;
import com.econference.auth.domain.EmailVerificationToken;
import com.econference.auth.domain.User;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationService implements VerifyEmailUseCase, ResendVerificationUseCase {

    private final EmailVerificationTokenRepositoryPort tokenRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final EmailConfig emailConfig;

    @Override
    @Transactional
    public void verify(String token) {
        EmailVerificationToken verificationToken = tokenRepositoryPort.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            throw new InvalidTokenException("Verification token already used");
        }
        if (verificationToken.isExpired()) {
            throw new InvalidTokenException("Verification token expired");
        }

        verificationToken.setUsedAt(Instant.now());
        tokenRepositoryPort.save(verificationToken);

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepositoryPort.save(user);
    }

    @Override
    @Transactional
    public void resend(String email) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        // Invalidate existing active tokens
        tokenRepositoryPort.findActiveByUserId(user.getId())
                .forEach(t -> {
                    t.setUsedAt(Instant.now());
                    tokenRepositoryPort.save(t);
                });

        String newToken = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(newToken)
                .expiresAt(Instant.now().plus(emailConfig.getVerificationExpiryHours(), ChronoUnit.HOURS))
                .build();

        tokenRepositoryPort.save(verificationToken);
        emailSenderPort.sendVerificationEmail(user.getEmail(), user.getFirstName(), newToken);
    }
}
