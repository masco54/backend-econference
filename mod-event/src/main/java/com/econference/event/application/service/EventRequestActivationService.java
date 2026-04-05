package com.econference.event.application.service;

import com.econference.event.application.exception.InvalidActivationTokenException;
import com.econference.event.application.exception.InvalidEventRequestStateException;
import com.econference.event.application.port.in.ActivateEventRequestUseCase;
import com.econference.event.application.port.in.ValidateActivationTokenUseCase;
import com.econference.event.application.port.out.EventRequestRepositoryPort;
import com.econference.event.domain.EventRequest;
import com.econference.event.domain.EventRequestStatus;
import com.econference.event.service.EventService;
import com.econference.shared.contract.EventRequestAccountProvisioningPort;
import com.econference.shared.exception.BusinessException;
import com.econference.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventRequestActivationService implements ValidateActivationTokenUseCase, ActivateEventRequestUseCase {

    private static final String ACTIVATION_SUCCESS_MESSAGE = "Ativação concluída com sucesso.";

    private final EventRequestRepositoryPort eventRequestRepositoryPort;
    private final EventRequestAccountProvisioningPort accountProvisioningPort;
    private final EventService eventService;

    @Override
    @Transactional(readOnly = true)
    public ValidateResult validate(UUID token) {
        EventRequest eventRequest = loadEventRequest(token);
        ensureActivationIsValid(eventRequest);

        boolean userExists = accountProvisioningPort.findUserByEmail(eventRequest.getRequesterEmail()).isPresent();
        return new ValidateResult(true, eventRequest.getRequesterEmail(), eventRequest.getEventName(), eventRequest.getId(), userExists);
    }

    @Override
    @Transactional
    public ActivateResult activate(ActivateCommand command) {
        EventRequest eventRequest = loadEventRequest(command.token());
        ensureActivationIsValid(eventRequest);

        if (eventRequest.getLinkedEventId() == null) {
            throw new InvalidEventRequestStateException("Approved event request is missing a linked event");
        }

        EventRequestAccountProvisioningPort.AccountProvisioningResult accountResult = accountProvisioningPort
                .findUserByEmail(eventRequest.getRequesterEmail())
                .map(existingUser -> activateExistingUser(eventRequest, existingUser, command.authenticatedUserId()))
                .orElseGet(() -> provisionNewUser(eventRequest, command.password(), command.confirmPassword()));

        eventService.assignOrganizer(eventRequest.getLinkedEventId(), accountResult.userId());
        eventRequest.setLinkedUserId(accountResult.userId());
        eventRequest.setActivationTokenUsedAt(Instant.now());
        eventRequest.addHistoryEntry(
                EventRequestStatus.APPROVED,
                accountResult.userCreated()
                        ? "Activation completed and organizer account created"
                        : "Activation completed and existing organizer account linked",
                accountResult.userId()
        );
        eventRequestRepositoryPort.save(eventRequest);

        return new ActivateResult(eventRequest.getLinkedEventId(), accountResult.userId(), ACTIVATION_SUCCESS_MESSAGE, accountResult.userCreated());
    }

    private EventRequestAccountProvisioningPort.AccountProvisioningResult activateExistingUser(
            EventRequest eventRequest,
            EventRequestAccountProvisioningPort.UserAccount existingUser,
            UUID authenticatedUserId
    ) {
        if (authenticatedUserId == null) {
            throw new BusinessException("Existing account activation requires authentication");
        }
        if (!existingUser.userId().equals(authenticatedUserId)) {
            throw new BusinessException("Authenticated user does not match the approved request email");
        }

        return accountProvisioningPort.linkApprovedRequestToExistingUser(
                new EventRequestAccountProvisioningPort.LinkApprovedRequestToExistingUserCommand(
                        existingUser.userId(),
                        eventRequest.getLinkedEventId(),
                        eventRequest.getRequesterEmail()
                )
        );
    }

    private EventRequestAccountProvisioningPort.AccountProvisioningResult provisionNewUser(
            EventRequest eventRequest,
            String password,
            String confirmPassword
    ) {
        if (password == null || password.isBlank() || confirmPassword == null || confirmPassword.isBlank()) {
            throw new BusinessException("Password and confirmation are required when no account exists");
        }
        if (!password.equals(confirmPassword)) {
            throw new BusinessException("Password confirmation does not match");
        }

        return accountProvisioningPort.provisionOrganizerAccount(
                new EventRequestAccountProvisioningPort.ProvisionOrganizerAccountCommand(
                        eventRequest.getRequesterFirstName(),
                        eventRequest.getRequesterLastName(),
                        eventRequest.getRequesterEmail(),
                        eventRequest.getRequesterPhone(),
                        password
                )
        );
    }

    private EventRequest loadEventRequest(UUID token) {
        return eventRequestRepositoryPort.findByActivationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Event request activation token", token));
    }

    private void ensureActivationIsValid(EventRequest eventRequest) {
        if (eventRequest.getStatus() != EventRequestStatus.APPROVED) {
            throw new InvalidEventRequestStateException("Event request is not approved for activation");
        }
        if (eventRequest.getActivationToken() == null) {
            throw new InvalidActivationTokenException("Activation token is not available for this request");
        }
        if (eventRequest.getActivationTokenUsedAt() != null) {
            throw new InvalidActivationTokenException("Activation token has already been used");
        }
        if (eventRequest.getActivationTokenExpiresAt() != null && eventRequest.getActivationTokenExpiresAt().isBefore(Instant.now())) {
            throw new InvalidActivationTokenException("Activation token has expired");
        }
    }
}