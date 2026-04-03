package com.econference.auth.application.port.in;

public interface ResetPasswordUseCase {

    void resetPassword(String token, String newPassword);
}
