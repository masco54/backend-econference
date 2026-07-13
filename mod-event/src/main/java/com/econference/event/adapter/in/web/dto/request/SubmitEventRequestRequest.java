package com.econference.event.adapter.in.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SubmitEventRequestRequest(
        @NotBlank @Size(min = 2, max = 50) String requesterFirstName,
        @NotBlank @Size(min = 2, max = 50) String requesterLastName,
        @Size(min = 3, max = 100) String requesterName,
        @NotBlank @Email String requesterEmail,
        @Pattern(regexp = "^$|^\\+?[1-9]\\d{10,14}$") String requesterPhone,
        @Size(max = 255) String requesterInstitution,
        @NotBlank @Size(min = 5, max = 200) String eventName,
        @NotBlank @Size(min = 10, max = 500) String eventTheme,
        @NotNull LocalDate eventStartDate,
        @NotNull LocalDate eventEndDate,
        @NotBlank @Size(min = 5, max = 200) String eventLocation,
        @NotBlank String eventType,
        boolean hasRegistration,
        boolean hasSubmission,
        boolean hasSite
) {
}