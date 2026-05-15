package com.example.ticketing.auth.controller;

import com.example.ticketing.auth.dto.LoginRequest;
import com.example.ticketing.auth.dto.LoginResponse;
import com.example.ticketing.auth.dto.SignupRequest;
import com.example.ticketing.auth.dto.SignupResponse;
import com.example.ticketing.auth.service.AuthService;
import com.example.ticketing.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success("회원가입이 완료되었습니다.", authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("로그인이 완료되었습니다.", authService.login(request));
    }
}
