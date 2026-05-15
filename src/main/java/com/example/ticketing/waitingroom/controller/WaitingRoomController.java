package com.example.ticketing.waitingroom.controller;

import com.example.ticketing.common.response.ApiResponse;
import com.example.ticketing.security.CustomUserDetails;
import com.example.ticketing.waitingroom.dto.WaitingRoomEnterRequest;
import com.example.ticketing.waitingroom.dto.WaitingRoomEnterResponse;
import com.example.ticketing.waitingroom.dto.WaitingRoomStatusResponse;
import com.example.ticketing.waitingroom.dto.WaitingRoomValidateRequest;
import com.example.ticketing.waitingroom.dto.WaitingRoomValidateResponse;
import com.example.ticketing.waitingroom.service.WaitingRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/waiting-room")
public class WaitingRoomController {

    private final WaitingRoomService waitingRoomService;

    @PostMapping("/enter")
    public ApiResponse<WaitingRoomEnterResponse> enter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WaitingRoomEnterRequest request
    ) {
        return ApiResponse.success("대기열에 진입했습니다.", waitingRoomService.enter(userDetails.getUserId(), request));
    }

    @GetMapping("/status")
    public ApiResponse<WaitingRoomStatusResponse> getStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long concertId,
            @RequestParam String waitingToken
    ) {
        return ApiResponse.success(waitingRoomService.getStatus(userDetails.getUserId(), concertId, waitingToken));
    }

    @PostMapping("/validate")
    public ApiResponse<WaitingRoomValidateResponse> validate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody WaitingRoomValidateRequest request
    ) {
        return ApiResponse.success("입장 토큰이 검증되었습니다.", waitingRoomService.validate(userDetails.getUserId(), request));
    }

    @PostMapping("/admit")
    public ApiResponse<Void> admit(@RequestParam Long concertId, @RequestParam(defaultValue = "100") long limit) {
        waitingRoomService.admit(concertId, limit);
        return ApiResponse.success("입장 가능 사용자를 갱신했습니다.", null);
    }
}
