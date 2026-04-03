package com.econference.auth.application.port.out;

public interface EmailSenderPort {

    void sendVerificationEmail(String to, String firstName, String token);

    void sendPasswordResetEmail(String to, String firstName, String token);
}
