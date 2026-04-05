package com.econference.application;

import com.econference.EConferenceApplication;
import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = EConferenceApplication.class)
@ActiveProfiles("test")
class AdminBootstrapIT {

    @Autowired
    private UserRepositoryPort userRepositoryPort;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldCreateVerifiedAdminUserForLocalLogin() {
        User admin = userRepositoryPort.findByEmail("admin@econference.local")
                .orElseThrow(() -> new AssertionError("Bootstrap admin user was not created"));

        assertThat(admin.isEmailVerified()).isTrue();
        assertThat(admin.getRoles()).contains(UserRole.ADMIN, UserRole.PARTICIPANT);
        assertThat(passwordEncoder.matches("Admin@123", admin.getPasswordHash())).isTrue();
    }
}