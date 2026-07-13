package com.econference.event.adapter.in.web.dto.request;

import jakarta.validation.constraints.Size;

public record ApproveEventRequestRequest(
        @Size(max = 1000) String adminNotes
) {
}