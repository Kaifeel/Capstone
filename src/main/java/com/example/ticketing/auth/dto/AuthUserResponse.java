package com.example.ticketing.auth.dto;

import com.example.ticketing.user.domain.User;
import com.example.ticketing.user.domain.UserRole;

public record AuthUserResponse(
        Long userId,
        String email,
        String name,
        UserRole role
) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
