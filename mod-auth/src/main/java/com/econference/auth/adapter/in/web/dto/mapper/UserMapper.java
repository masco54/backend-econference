package com.econference.auth.adapter.in.web.dto.mapper;

import com.econference.auth.adapter.in.web.dto.response.LoginResponse;
import com.econference.auth.adapter.in.web.dto.response.PublicUserResponse;
import com.econference.auth.adapter.in.web.dto.response.UserResponse;
import com.econference.auth.domain.User;
import com.econference.auth.domain.UserRole;

import java.util.Set;
import java.util.stream.Collectors;

public class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.isEmailVerified(),
                mapRoles(user.getRoles())
        );
    }

    public static LoginResponse toLoginResponse(User user) {
        return new LoginResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                mapRoles(user.getRoles())
        );
    }

    public static PublicUserResponse toPublicUserResponse(User user) {
        return new PublicUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                mapRoles(user.getRoles())
        );
    }

    private static Set<String> mapRoles(Set<UserRole> roles) {
        return roles.stream()
                .map(role -> role.name().toLowerCase())
                .collect(Collectors.toSet());
    }
}
