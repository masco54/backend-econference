package com.econference.auth.application.port.in;

import java.util.UUID;

public interface ChangePasswordUseCase {

    void changePassword(UUID userId, String currentPassword, String newPassword);
}
