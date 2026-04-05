package com.econference.auth.config;

import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.email:admin@econference.local}")
    private String email;

    @Value("${app.bootstrap.admin.password:Admin@123}")
    private String password;

    @Value("${app.bootstrap.admin.first-name:Admin}")
    private String firstName;

    @Value("${app.bootstrap.admin.last-name:Econference}")
    private String lastName;

    @Value("${app.bootstrap.admin.phone:}")
    private String phone;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.debug("Bootstrap do admin desabilitado por configuração");
            return;
        }

        validateConfiguration();

        userRepositoryPort.findByEmail(email.trim())
                .ifPresentOrElse(this::promoteExistingUser, this::createBootstrapAdmin);
    }

    private void validateConfiguration() {
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("app.bootstrap.admin.email must be configured when bootstrap admin is enabled");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("app.bootstrap.admin.password must be configured when bootstrap admin is enabled");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalStateException("app.bootstrap.admin.first-name must be configured when bootstrap admin is enabled");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalStateException("app.bootstrap.admin.last-name must be configured when bootstrap admin is enabled");
        }
    }

    private void createBootstrapAdmin() {
        Set<UserRole> roles = new HashSet<>();
        roles.add(UserRole.ADMIN);
        roles.add(UserRole.PARTICIPANT);

        User user = User.builder()
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .email(email.trim())
                .phone(normalizePhone(phone))
                .passwordHash(passwordEncoder.encode(password))
                .emailVerified(true)
                .roles(roles)
                .build();

        userRepositoryPort.save(user);
        log.info("Usuário administrador bootstrap criado para email={}", email.trim());
    }

    private void promoteExistingUser(User user) {
        boolean changed = false;

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            changed = true;
        }

        if (user.getRoles() == null) {
            user.setRoles(new HashSet<>());
            changed = true;
        }

        if (user.getRoles().add(UserRole.ADMIN)) {
            changed = true;
        }

        if (user.getRoles().add(UserRole.PARTICIPANT)) {
            changed = true;
        }

        if (changed) {
            userRepositoryPort.save(user);
            log.info("Usuário existente promovido para administrador bootstrap email={}", user.getEmail());
            return;
        }

        log.info("Usuário administrador bootstrap já disponível email={}", user.getEmail());
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }

        String trimmed = rawPhone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}