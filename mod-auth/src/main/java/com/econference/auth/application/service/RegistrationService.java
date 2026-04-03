package com.econference.auth.application.service;

import com.econference.auth.application.exception.EmailAlreadyExistsException;
import com.econference.auth.application.port.in.RegisterUserUseCase;
import com.econference.auth.application.port.out.EmailSenderPort;
import com.econference.auth.application.port.out.EmailVerificationTokenRepositoryPort;
import com.econference.auth.application.port.out.InvitationTokenPort;
import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.config.EmailConfig;
import com.econference.auth.domain.EmailVerificationToken;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService implements RegisterUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final EmailVerificationTokenRepositoryPort emailVerificationTokenRepositoryPort;
    private final EmailSenderPort emailSenderPort;
    private final InvitationTokenPort invitationTokenPort;
    private final PasswordEncoder passwordEncoder;
    private final EmailConfig emailConfig;

    @Override
    @Transactional
    public RegisterResult register(String firstName, String lastName, String email, String password,
                                   String phone, String invitationToken, String coOrganizerToken) {
        if (userRepositoryPort.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.PARTICIPANT);

        boolean reviewerLinked = false;
        boolean coOrganizerLinked = false;

        if (invitationToken != null && !invitationToken.isBlank()) {
            reviewerLinked = invitationTokenPort.validateReviewerToken(invitationToken);
        }
        if (coOrganizerToken != null && !coOrganizerToken.isBlank()) {
            coOrganizerLinked = invitationTokenPort.validateCoOrganizerToken(coOrganizerToken);
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(password))
                .emailVerified(false)
                .roles(roles)
                .build();

        user = userRepositoryPort.save(user);

        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(Instant.now().plus(emailConfig.getVerificationExpiryHours(), ChronoUnit.HOURS))
                .build();

        emailVerificationTokenRepositoryPort.save(verificationToken);
        emailSenderPort.sendVerificationEmail(email, firstName, token);

        return new RegisterResult(user.getId(), reviewerLinked, coOrganizerLinked);
    }
}
