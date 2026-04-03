package com.econference.auth.application.port.in;

import com.econference.auth.domain.User;

import java.util.UUID;

public interface UpdateProfileUseCase {

    User updateProfile(UUID authenticatedUserId, UUID targetUserId, String firstName, String lastName, String phone);
}
