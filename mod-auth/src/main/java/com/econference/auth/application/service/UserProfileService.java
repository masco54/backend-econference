package com.econference.auth.application.service;

import com.econference.auth.application.port.in.GetMyRolesUseCase;
import com.econference.auth.application.port.in.GetPublicUserUseCase;
import com.econference.auth.application.port.in.UpdateProfileUseCase;
import com.econference.auth.application.port.out.UserRepositoryPort;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserRole;
import com.econference.shared.exception.ForbiddenException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService implements GetPublicUserUseCase, UpdateProfileUseCase, GetMyRolesUseCase {

    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public User getPublicUser(UUID userId) {
        return userRepositoryPort.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public User updateProfile(UUID authenticatedUserId, UUID targetUserId, String firstName, String lastName, String phone) {
        if (!authenticatedUserId.equals(targetUserId)) {
            throw new ForbiddenException("You can only update your own profile");
        }

        User user = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }

        return userRepositoryPort.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<UserRole> getRoles(UUID userId) {
        User user = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return user.getRoles();
    }
}
