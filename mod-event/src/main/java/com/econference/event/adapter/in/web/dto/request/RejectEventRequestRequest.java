package com.econference.event.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectEventRequestRequest(
        @NotBlank @Size(min = 10, max = 1000) String reason
) {
}