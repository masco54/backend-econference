package com.econference.event.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TrackEventRequestRequest(
        @NotBlank String trackingCode,
        @NotBlank @Email String email
) {
}