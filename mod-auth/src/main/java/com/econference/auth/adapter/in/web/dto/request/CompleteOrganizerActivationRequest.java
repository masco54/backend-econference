package com.econference.auth.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CompleteOrganizerActivationRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 100) String password,
        @Pattern(regexp = "^$|^\\+?[1-9]\\d{10,14}$") String phone
) {}
