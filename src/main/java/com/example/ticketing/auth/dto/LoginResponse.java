package com.example.ticketing.auth.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        AuthUserResponse user
) {
}
