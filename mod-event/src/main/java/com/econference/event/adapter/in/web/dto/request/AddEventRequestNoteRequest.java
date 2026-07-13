package com.econference.event.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddEventRequestNoteRequest(
        @NotBlank @Size(min = 5, max = 1000) String note
) {
}