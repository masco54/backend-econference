package com.econference.auth.adapter.in.web.dto.response;

import java.util.Set;
import java.util.UUID;

public record ValidateOrganizerActivationTokenResponse(
        boolean valid,
        String reason,
        String firstName,
        String lastName,
        String email,
        String phone,
        String eventName
) {}
