package com.econference.auth.application.port.in;

import com.econference.auth.domain.User;

public interface LoginUseCase {

    LoginResult login(String email, String password);

    record LoginResult(User user, String rawJwt) {
    }
}
