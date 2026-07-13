package com.econference.auth.application.service;

import com.econference.auth.application.exception.InvalidTokenException;
import com.econference.auth.application.port.in.CompleteOrganizerActivationUseCase;
import com.econference.auth.application.port.in.ValidateOrganizerActivationTokenUseCase;
import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.application.port.out.UserSessionRepositoryPort;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserSession;
import com.econference.auth.security.JwtAuthenticationFilter;
import com.econference.auth.security.JwtService;
import com.econference.shared.contract.EventRequestActivationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Unified organizer activation service. Delegates all activation operations to
 * mod-event via EventRequestActivationPort, so that event_requests is the single
 * source of truth for activation tokens.
 */
@Service
@RequiredArgsConstructor
public class OrganizerActivationService
        implements ValidateOrganizerActivationTokenUseCase, CompleteOrganizerActivationUseCase {

    private final EventRequestActivationPort activationPort;
    private final UserRepositoryPort userRepositoryPort;
    private final UserSessionRepositoryPort userSessionRepositoryPort;
    private final JwtService jwtService;

    @Override
    public ValidationResult validate(String tokenString) {
        UUID token = parseToken(tokenString);
        if (token == null) {
            return ValidationResult.invalid("not-found");
        }
        EventRequestActivationPort.ValidateActivationResult result = activationPort.validateActivation(token);
        if (!result.valid()) {
            return ValidationResult.invalid(result.reason());
        }
        return ValidationResult.valid(
                result.firstName(), result.lastName(),
                result.email(), result.phone(), result.eventName());
    }

    @Override
    @Transactional
    public ActivationResult complete(CompleteOrganizerActivationCommand command) {
        UUID token = parseToken(command.token());
        if (token == null) {
            throw new InvalidTokenException("Token de ativação inválido.");
        }
        EventRequestActivationPort.CompleteActivationResult result = activationPort.completeActivation(
                new EventRequestActivationPort.CompleteActivationCommand(token, command.password(), command.phone())
        );
        User user = userRepositoryPort.findById(result.userId())
                .orElseThrow(() -> new InvalidTokenException("Usuário não encontrado após ativação."));
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.name().toLowerCase())
                .collect(Collectors.toSet());

        String jwt = jwtService.generateToken(user.getId(), roles);
        UserSession session = UserSession.builder()
                .user(user)
                .tokenHash(JwtAuthenticationFilter.hashToken(jwt))
                .expiresAt(jwtService.getExpiration(jwt))
                .build();
        userSessionRepositoryPort.save(session);

        return new ActivationResult(
                user.getId(), roles, result.eventId(),
                user.getFirstName(), user.getLastName(),
                user.getEmail(), user.getPhone(), user.isEmailVerified(), jwt);
    }

    private UUID parseToken(String tokenString) {
        if (tokenString == null || tokenString.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(tokenString);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
