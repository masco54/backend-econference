package com.econference.auth.application.service;

import com.econference.auth.application.exception.InvalidTokenException;
import com.econference.auth.application.port.in.ChangePasswordUseCase;
import com.econference.auth.application.port.in.ForgotPasswordUseCase;
import com.econference.auth.application.port.in.ResetPasswordUseCase;
import com.econference.auth.application.port.out.EmailSenderPort;
import com.econference.auth.application.port.out.PasswordResetTokenRepositoryPort;
import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.application.port.out.UserSessionRepositoryPort;
import com.econference.auth.config.EmailConfig;
import com.econference.auth.domain.PasswordResetToken;
import com.econference.auth.domain.User;
import com.econference.shared.exception.AuthException;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService implements ForgotPasswordUseCase, ResetPasswordUseCase, ChangePasswordUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordResetTokenRepositoryPort tokenRepositoryPort;
    private final UserSessionRepositoryPort userSessionRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final PasswordEncoder passwordEncoder;
    private final EmailConfig emailConfig;

    @Override
    @Transactional
    public void forgotPassword(String email) {
        // Always return success to prevent account enumeration
        userRepositoryPort.findByEmail(email).ifPresent(user -> {
            // Invalidate existing active tokens
            tokenRepositoryPort.findActiveByUserId(user.getId())
                    .forEach(t -> {
                        t.setUsedAt(Instant.now());
                        tokenRepositoryPort.save(t);
                    });

            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plus(emailConfig.getResetTokenExpiryHours(), ChronoUnit.HOURS))
                    .build();

            tokenRepositoryPort.save(resetToken);
            emailSenderPort.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
        });
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepositoryPort.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid reset token"));

        if (resetToken.isUsed()) {
            throw new InvalidTokenException("Reset token already used");
        }
        if (resetToken.isExpired()) {
            throw new InvalidTokenException("Reset token expired");
        }

        resetToken.setUsedAt(Instant.now());
        tokenRepositoryPort.save(resetToken);

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepositoryPort.save(user);

        // Invalidate all active sessions for this user
        userSessionRepositoryPort.invalidateAllByUserId(user.getId());
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String currentPassword, String newPassword) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepositoryPort.save(user);
    }
}
