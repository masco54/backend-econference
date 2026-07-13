package com.econference.auth.adapter.in.web;

import com.econference.auth.adapter.in.web.dto.request.CompleteOrganizerActivationRequest;
import com.econference.auth.adapter.in.web.dto.response.CompleteOrganizerActivationResponse;
import com.econference.auth.adapter.in.web.dto.response.ValidateOrganizerActivationTokenResponse;
import com.econference.auth.application.port.in.CompleteOrganizerActivationUseCase;
import com.econference.auth.application.port.in.CompleteOrganizerActivationUseCase.CompleteOrganizerActivationCommand;
import com.econference.auth.application.port.in.ValidateOrganizerActivationTokenUseCase;
import com.econference.auth.application.port.in.ValidateOrganizerActivationTokenUseCase.ValidationResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/organizer-activation")
@RequiredArgsConstructor
@Slf4j
public class OrganizerActivationController {

    private final ValidateOrganizerActivationTokenUseCase validateUseCase;
    private final CompleteOrganizerActivationUseCase completeUseCase;

    @GetMapping("/validate")
    public ResponseEntity<ValidateOrganizerActivationTokenResponse> validate(
            @RequestParam String token) {
        log.info("Validating organizer activation token");
        ValidationResult result = validateUseCase.validate(token);
        return ResponseEntity.ok(new ValidateOrganizerActivationTokenResponse(
                result.valid(),
                result.reason(),
                result.firstName(),
                result.lastName(),
                result.email(),
                result.phone(),
                result.eventName()
        ));
    }

    @PostMapping("/complete")
    public ResponseEntity<CompleteOrganizerActivationResponse> complete(
            @Valid @RequestBody CompleteOrganizerActivationRequest request) {
        log.info("Completing organizer activation for token={}", request.token().substring(0, Math.min(8, request.token().length())) + "…");
        var result = completeUseCase.complete(
                new CompleteOrganizerActivationCommand(request.token(), request.password(), request.phone()));

        var userInfo = new CompleteOrganizerActivationResponse.UserInfo(
                result.userId(),
                result.firstName(),
                result.lastName(),
                result.email(),
                result.phone(),
                result.roles(),
                result.emailVerified()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CompleteOrganizerActivationResponse(
                        result.userId(),
                        result.roles(),
                        result.eventId(),
                        userInfo
                ));
    }
}
