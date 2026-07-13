package com.econference.event.adapter.in.web;

import com.econference.event.adapter.in.web.dto.request.ActivateEventRequestRequest;
import com.econference.event.adapter.in.web.dto.request.SubmitEventRequestRequest;
import com.econference.event.adapter.in.web.dto.request.TrackEventRequestRequest;
import com.econference.event.adapter.in.web.dto.response.ActivateEventRequestResponse;
import com.econference.event.adapter.in.web.dto.response.SubmitEventRequestResponse;
import com.econference.event.adapter.in.web.dto.response.TrackEventRequestResponse;
import com.econference.event.adapter.in.web.dto.response.ValidateActivationTokenResponse;
import com.econference.event.application.port.in.ActivateEventRequestUseCase;
import com.econference.event.application.port.in.SubmitEventRequestUseCase;
import com.econference.event.application.port.in.TrackEventRequestUseCase;
import com.econference.event.application.port.in.ValidateActivationTokenUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/public/event-requests")
@RequiredArgsConstructor
public class EventRequestPublicController {

    private final SubmitEventRequestUseCase submitEventRequestUseCase;
        private final TrackEventRequestUseCase trackEventRequestUseCase;
        private final ValidateActivationTokenUseCase validateActivationTokenUseCase;
        private final ActivateEventRequestUseCase activateEventRequestUseCase;

    @PostMapping
    public ResponseEntity<SubmitEventRequestResponse> submit(@Valid @RequestBody SubmitEventRequestRequest request) {
        SubmitEventRequestUseCase.SubmitResult result = submitEventRequestUseCase.submit(
                new SubmitEventRequestUseCase.SubmitCommand(
                        request.requesterFirstName(),
                        request.requesterLastName(),
                        request.requesterName(),
                        request.requesterEmail(),
                        request.requesterPhone(),
                        request.requesterInstitution(),
                        request.eventName(),
                        request.eventTheme(),
                        request.eventStartDate(),
                        request.eventEndDate(),
                        request.eventLocation(),
                        request.eventType(),
                        request.hasRegistration(),
                        request.hasSubmission(),
                        request.hasSite()
                )
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new SubmitEventRequestResponse(result.id(), result.trackingCode(), result.message()));
    }

    @PostMapping("/track")
    public ResponseEntity<TrackEventRequestResponse> track(@Valid @RequestBody TrackEventRequestRequest request) {
        TrackEventRequestUseCase.TrackResult result = trackEventRequestUseCase.track(
                new TrackEventRequestUseCase.TrackCommand(request.trackingCode(), request.email())
        );

        return ResponseEntity.ok(new TrackEventRequestResponse(
                result.trackingCode(),
                result.status(),
                result.eventName(),
                result.statusHistory().stream()
                        .map(item -> new TrackEventRequestResponse.StatusHistoryEntryResponse(
                                item.status(),
                                item.timestamp(),
                                item.message()
                        ))
                        .toList(),
                result.activationUrl(),
                result.createdAt()
        ));
    }

    @GetMapping("/activate")
    public ResponseEntity<ValidateActivationTokenResponse> validateActivationToken(@RequestParam UUID token) {
        ValidateActivationTokenUseCase.ValidateResult result = validateActivationTokenUseCase.validate(token);
        return ResponseEntity.ok(new ValidateActivationTokenResponse(
                result.valid(),
                result.email(),
                result.eventName(),
                result.eventRequestId(),
                result.userExists()
        ));
    }

    @PostMapping("/activate")
    public ResponseEntity<ActivateEventRequestResponse> activate(
            @Valid @RequestBody ActivateEventRequestRequest request,
            @AuthenticationPrincipal(expression = "userId") UUID authenticatedUserId
    ) {
        ActivateEventRequestUseCase.ActivateResult result = activateEventRequestUseCase.activate(
                new ActivateEventRequestUseCase.ActivateCommand(
                        request.token(),
                        request.password(),
                        request.confirmPassword(),
                        authenticatedUserId
                )
        );

        return ResponseEntity.ok(new ActivateEventRequestResponse(
                result.eventId(),
                result.userId(),
                result.message(),
                result.userCreated()
        ));
    }
}