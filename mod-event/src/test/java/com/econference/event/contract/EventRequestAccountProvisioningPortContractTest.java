package com.econference.event.contract;

import com.econference.shared.contract.EventRequestAccountProvisioningPort;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventRequestAccountProvisioningPortContractTest {

    @Test
    void shouldExposeTheConsumerAssumptionsForProvisioningAndExistingAccountLinking() {
        FakeProvisioningPort port = new FakeProvisioningPort();
        EventRequestAccountProvisioningPort.ProvisionOrganizerAccountCommand provisionCommand =
                new EventRequestAccountProvisioningPort.ProvisionOrganizerAccountCommand(
                        "Ana",
                        "Silva",
                        "ana.silva@example.com",
                        "+5511999999999",
                        "NovaSenha123"
                );

        EventRequestAccountProvisioningPort.AccountProvisioningResult provisioned = port.provisionOrganizerAccount(provisionCommand);
        Optional<EventRequestAccountProvisioningPort.UserAccount> existingUser = port.findUserByEmail("ana.silva@example.com");
        EventRequestAccountProvisioningPort.AccountProvisioningResult linked = port.linkApprovedRequestToExistingUser(
                new EventRequestAccountProvisioningPort.LinkApprovedRequestToExistingUserCommand(
                        provisioned.userId(),
                        UUID.randomUUID(),
                        "ana.silva@example.com"
                )
        );

        assertThat(existingUser).isPresent();
        assertThat(provisioned.userCreated()).isTrue();
        assertThat(provisioned.roles()).contains("organizer");
        assertThat(linked.userCreated()).isFalse();
        assertThat(linked.userId()).isEqualTo(provisioned.userId());
    }

    private static final class FakeProvisioningPort implements EventRequestAccountProvisioningPort {

        private final Map<String, UserAccount> usersByEmail = new HashMap<>();

        @Override
        public Optional<UserAccount> findUserByEmail(String email) {
            return Optional.ofNullable(usersByEmail.get(email.toLowerCase()));
        }

        @Override
        public AccountProvisioningResult provisionOrganizerAccount(ProvisionOrganizerAccountCommand command) {
            UUID userId = UUID.randomUUID();
            UserAccount user = new UserAccount(userId, command.email(), Set.of("participant", "organizer"), true);
            usersByEmail.put(command.email().toLowerCase(), user);
            return new AccountProvisioningResult(userId, true, user.roles());
        }

        @Override
        public AccountProvisioningResult linkApprovedRequestToExistingUser(LinkApprovedRequestToExistingUserCommand command) {
            UserAccount user = usersByEmail.values().stream()
                    .filter(candidate -> candidate.userId().equals(command.userId()))
                    .findFirst()
                    .orElseThrow();
            return new AccountProvisioningResult(user.userId(), false, user.roles());
        }
    }
}