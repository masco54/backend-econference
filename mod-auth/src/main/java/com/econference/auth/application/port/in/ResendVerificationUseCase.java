package com.econference.auth.application.port.in;

public interface ResendVerificationUseCase {

    void resend(String email);
}
