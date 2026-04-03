package com.econference.auth.adapter.out.email;

import com.econference.auth.application.port.out.EmailSenderPort;
import com.econference.auth.config.EmailConfig;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SpringMailEmailSenderAdapter implements EmailSenderPort {

    private final JavaMailSender mailSender;
    private final EmailConfig emailConfig;

    @Override
    @Async
    public void sendVerificationEmail(String to, String firstName, String token) {
        String subject = "Verificação de Email - eConference";
        String html = EmailTemplates.verificationEmail(firstName, token, emailConfig);
        sendHtmlEmail(to, subject, html);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        String subject = "Redefinição de Senha - eConference";
        String html = EmailTemplates.passwordResetEmail(firstName, token, emailConfig);
        sendHtmlEmail(to, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(emailConfig.getFrom());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
