package com.econference.auth.adapter.in.web.dto.response;

import java.util.Set;
import java.util.UUID;

public record PublicUserResponse(
        UUID id,
        String firstName,
        String lastName,
        Set<String> roles
) {
}
