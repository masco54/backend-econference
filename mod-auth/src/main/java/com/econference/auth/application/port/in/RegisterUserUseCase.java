package com.econference.auth.application.port.in;

import java.util.UUID;

public interface RegisterUserUseCase {

    RegisterResult register(String firstName, String lastName, String email, String password,
                            String phone, String invitationToken, String coOrganizerToken);

    record RegisterResult(UUID userId, boolean reviewerLinked, boolean coOrganizerLinked) {
    }
}
