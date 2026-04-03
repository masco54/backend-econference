package com.econference.auth.adapter.in.web.dto.response;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        boolean reviewerLinked,
        boolean coOrganizerLinked
) {
}
