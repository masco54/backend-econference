package com.econference.auth.adapter.in.web;

import com.econference.auth.adapter.in.web.dto.mapper.UserMapper;
import com.econference.auth.adapter.in.web.dto.request.UpdateProfileRequest;
import com.econference.auth.adapter.in.web.dto.response.PublicUserResponse;
import com.econference.auth.adapter.in.web.dto.response.RolesResponse;
import com.econference.auth.adapter.in.web.dto.response.UserResponse;
import com.econference.auth.application.port.in.GetMyRolesUseCase;
import com.econference.auth.application.port.in.GetPublicUserUseCase;
import com.econference.auth.application.port.in.UpdateProfileUseCase;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserRole;
import com.econference.auth.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final GetPublicUserUseCase getPublicUserUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final GetMyRolesUseCase getMyRolesUseCase;

    @GetMapping("/me/roles")
    public ResponseEntity<RolesResponse> getMyRoles(@AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        Set<UserRole> roles = getMyRolesUseCase.getRoles(principal.getUserId());
        Set<String> roleNames = roles.stream()
                .map(role -> role.name().toLowerCase())
                .collect(Collectors.toSet());
        return ResponseEntity.ok(new RolesResponse(roleNames));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<PublicUserResponse> getUserById(@PathVariable UUID userId) {
        User user = getPublicUserUseCase.getPublicUser(userId);
        return ResponseEntity.ok(UserMapper.toPublicUserResponse(user));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateProfile(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = updateProfileUseCase.updateProfile(
                principal.getUserId(), userId,
                request.firstName(), request.lastName(), request.phone());
        return ResponseEntity.ok(UserMapper.toUserResponse(user));
    }
}
