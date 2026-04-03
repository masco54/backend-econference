package com.econference.auth.adapter.in.web.dto.response;

import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        boolean emailVerified,
        Set<String> roles
) {
}
