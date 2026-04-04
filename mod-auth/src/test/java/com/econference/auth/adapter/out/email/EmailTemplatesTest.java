package com.econference.auth.adapter.out.email;

import com.econference.auth.config.EmailConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplatesTest {

    @Test
    void verificationEmailShouldReplaceTemplateMarkers() {
        EmailConfig emailConfig = buildEmailConfig();

        String html = EmailTemplates.verificationEmail("Marco", "verification-token", emailConfig);

        assertTrue(html.contains("Olá Marco,"));
        assertTrue(html.contains("http://localhost:3000/verify-email?token=verification-token"));
        assertTrue(html.contains("24 horas"));
        assertFalse(html.contains("{{FIRST_NAME}}"));
        assertFalse(html.contains("{{ACTION_URL}}"));
        assertFalse(html.contains("{{EXPIRY_HOURS}}"));
    }

    @Test
    void passwordResetEmailShouldReplaceTemplateMarkers() {
        EmailConfig emailConfig = buildEmailConfig();

        String html = EmailTemplates.passwordResetEmail("Marco", "reset-token", emailConfig);

        assertTrue(html.contains("Olá Marco,"));
        assertTrue(html.contains("http://localhost:3000/reset-password?token=reset-token"));
        assertTrue(html.contains("2 hora(s)"));
        assertFalse(html.contains("{{FIRST_NAME}}"));
        assertFalse(html.contains("{{ACTION_URL}}"));
        assertFalse(html.contains("{{EXPIRY_HOURS}}"));
    }

    private EmailConfig buildEmailConfig() {
        EmailConfig emailConfig = new EmailConfig();
        emailConfig.setBaseUrl("http://localhost:4200");
        emailConfig.setVerificationExpiryHours(24);
        emailConfig.setResetTokenExpiryHours(2);
        return emailConfig;
    }
}