package com.econference.auth.application.port.in;

public interface LogoutUseCase {

    void logout(String rawJwt);
}
