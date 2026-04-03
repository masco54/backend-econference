package com.econference.auth.adapter.in.web.dto.response;

import java.util.Set;

public record RolesResponse(
        Set<String> roles
) {
}
