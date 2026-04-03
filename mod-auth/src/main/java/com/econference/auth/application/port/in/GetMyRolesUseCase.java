package com.econference.auth.application.port.in;

import com.econference.auth.domain.UserRole;

import java.util.Set;
import java.util.UUID;

public interface GetMyRolesUseCase {

    Set<UserRole> getRoles(UUID userId);
}
