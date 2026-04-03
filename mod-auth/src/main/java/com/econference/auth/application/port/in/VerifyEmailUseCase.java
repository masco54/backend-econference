package com.econference.auth.application.port.in;

public interface VerifyEmailUseCase {

    void verify(String token);
}
