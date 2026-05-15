package com.example.ticketing.auth.dto;

import com.example.ticketing.user.domain.User;
import com.example.ticketing.user.domain.UserRole;

public record SignupResponse(
        Long userId,
        String email,
        String name,
        UserRole role
) {

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getId(), user.getEmail(), user.getName(), user.getRole());
    }
}
