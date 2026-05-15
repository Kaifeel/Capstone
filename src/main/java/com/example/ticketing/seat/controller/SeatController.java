package com.example.ticketing.seat.controller;

import com.example.ticketing.common.response.ApiResponse;
import com.example.ticketing.seat.domain.SeatStatus;
import com.example.ticketing.seat.dto.SeatBulkCreateRequest;
import com.example.ticketing.seat.dto.SeatBulkCreateResponse;
import com.example.ticketing.seat.dto.SeatListResponse;
import com.example.ticketing.seat.dto.SeatResponse;
import com.example.ticketing.seat.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @PostMapping("/api/v1/concerts/{concertId}/seats")
    public ApiResponse<SeatBulkCreateResponse> createSeats(
            @PathVariable Long concertId,
            @Valid @RequestBody SeatBulkCreateRequest request
    ) {
        return ApiResponse.success("좌석이 생성되었습니다.", seatService.createSeats(concertId, request));
    }

    @GetMapping("/api/v1/concerts/{concertId}/seats")
    public ApiResponse<SeatListResponse> getSeats(
            @PathVariable Long concertId,
            @RequestParam(required = false) SeatStatus status
    ) {
        return ApiResponse.success(seatService.getSeats(concertId, status));
    }

    @GetMapping("/api/v1/seats/{seatId}")
    public ApiResponse<SeatResponse> getSeat(@PathVariable Long seatId) {
        return ApiResponse.success(seatService.getSeat(seatId));
    }
}
