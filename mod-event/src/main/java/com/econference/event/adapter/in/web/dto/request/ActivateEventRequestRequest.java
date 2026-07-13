package com.econference.event.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ActivateEventRequestRequest(
        @NotNull UUID token,
        @Size(min = 8, max = 255) String password,
        @Size(min = 8, max = 255) String confirmPassword
) {
}