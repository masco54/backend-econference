package com.econference.auth.application.port.in;

public interface ValidateOrganizerActivationTokenUseCase {

    ValidationResult validate(String token);

    record ValidationResult(
            boolean valid,
            String reason,
            String firstName,
            String lastName,
            String email,
            String phone,
            String eventName
    ) {
        public static ValidationResult valid(String firstName, String lastName, String email,
                                             String phone, String eventName) {
            return new ValidationResult(true, null, firstName, lastName, email, phone, eventName);
        }

        public static ValidationResult invalid(String reason) {
            return new ValidationResult(false, reason, null, null, null, null, null);
        }
    }
}
