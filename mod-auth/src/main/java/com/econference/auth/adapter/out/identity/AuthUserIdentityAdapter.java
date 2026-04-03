package com.econference.auth.adapter.out.identity;

import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.shared.contract.UserIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthUserIdentityAdapter implements UserIdentityPort {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    public Optional<UserIdentity> findById(UUID userId) {
        return userRepositoryPort.findById(userId)
                .map(user -> new UserIdentity(
                        user.getId(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRoles().stream()
                                .map(role -> role.name().toLowerCase())
                                .collect(Collectors.toSet())
                ));
    }
}
