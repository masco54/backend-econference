package com.econference.auth.application.service;

import com.econference.auth.application.exception.EmailNotVerifiedException;
import com.econference.auth.application.port.in.GetCurrentUserUseCase;
import com.econference.auth.application.port.in.LoginUseCase;
import com.econference.auth.application.port.in.LogoutUseCase;
import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.application.port.out.UserSessionRepositoryPort;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserRole;
import com.econference.auth.domain.UserSession;
import com.econference.auth.security.JwtAuthenticationFilter;
import com.econference.auth.security.JwtService;
import com.econference.shared.exception.AuthException;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthenticationService implements LoginUseCase, LogoutUseCase, GetCurrentUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final UserSessionRepositoryPort userSessionRepositoryPort;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public LoginResult login(String email, String password) {
        User user = userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new AuthException("Invalid credentials");
        }

        Set<String> roleNames = user.getRoles().stream()
                .map(UserRole::name)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        String jwt = jwtService.generateToken(user.getId(), roleNames);

        UserSession session = UserSession.builder()
                .user(user)
                .tokenHash(JwtAuthenticationFilter.hashToken(jwt))
                .expiresAt(jwtService.getExpiration(jwt))
                .build();

        userSessionRepositoryPort.save(session);

        return new LoginResult(user, jwt);
    }

    @Override
    @Transactional
    public void logout(String rawJwt) {
        String tokenHash = JwtAuthenticationFilter.hashToken(rawJwt);
        userSessionRepositoryPort.findActiveByTokenHash(tokenHash)
                .ifPresent(session -> {
                    session.setInvalidatedAt(java.time.Instant.now());
                    userSessionRepositoryPort.save(session);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
