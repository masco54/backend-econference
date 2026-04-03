package com.econference.shared.contract;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface UserIdentityPort {

    Optional<UserIdentity> findById(UUID userId);

    record UserIdentity(UUID id, String firstName, String lastName, Set<String> roles) {
    }
}
