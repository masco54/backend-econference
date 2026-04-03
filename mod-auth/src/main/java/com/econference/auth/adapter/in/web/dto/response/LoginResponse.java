package com.econference.auth.adapter.in.web.dto.response;

import java.util.Set;
import java.util.UUID;

public record LoginResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Set<String> roles
) {
}
