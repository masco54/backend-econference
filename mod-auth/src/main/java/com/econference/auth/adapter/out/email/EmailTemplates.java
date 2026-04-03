package com.econference.auth.adapter.out.email;

import com.econference.auth.config.EmailConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;

public class EmailTemplates {

  private static final String VERIFICATION_TEMPLATE_PATH = "templates/email/verification-email.html";
  private static final String PASSWORD_RESET_TEMPLATE_PATH = "templates/email/password-reset-email.html";
  private static final String FIRST_NAME_PLACEHOLDER = "{{FIRST_NAME}}";
  private static final String ACTION_URL_PLACEHOLDER = "{{ACTION_URL}}";
  private static final String EXPIRY_HOURS_PLACEHOLDER = "{{EXPIRY_HOURS}}";

    private EmailTemplates() {
    }

    public static String verificationEmail(String firstName, String token, EmailConfig emailConfig) {
        String verificationUrl = emailConfig.getBaseUrl() + "/verify-email?token=" + token;
    return renderTemplate(
        VERIFICATION_TEMPLATE_PATH,
        Map.of(
            FIRST_NAME_PLACEHOLDER, safeValue(firstName),
            ACTION_URL_PLACEHOLDER, safeValue(verificationUrl),
            EXPIRY_HOURS_PLACEHOLDER, String.valueOf(emailConfig.getVerificationExpiryHours())));
    }

    public static String passwordResetEmail(String firstName, String token, EmailConfig emailConfig) {
        String resetUrl = emailConfig.getBaseUrl() + "/reset-password?token=" + token;
    return renderTemplate(
        PASSWORD_RESET_TEMPLATE_PATH,
        Map.of(
            FIRST_NAME_PLACEHOLDER, safeValue(firstName),
            ACTION_URL_PLACEHOLDER, safeValue(resetUrl),
            EXPIRY_HOURS_PLACEHOLDER, String.valueOf(emailConfig.getResetTokenExpiryHours())));
  }

  private static String renderTemplate(String templatePath, Map<String, String> replacements) {
    String renderedTemplate = loadTemplate(templatePath);
    for (Map.Entry<String, String> replacement : replacements.entrySet()) {
      renderedTemplate = renderedTemplate.replace(replacement.getKey(), replacement.getValue());
    }
    return renderedTemplate;
  }

  private static String loadTemplate(String templatePath) {
    ClassPathResource resource = new ClassPathResource(templatePath);
    try (InputStream inputStream = resource.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException exception) {
      throw new IllegalStateException("Failed to load email template: " + templatePath, exception);
    }
  }

  private static String safeValue(String value) {
    return value == null ? "" : value;
    }
}
