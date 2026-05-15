package com.example.ticketing.reservation.controller;

import com.example.ticketing.common.response.ApiResponse;
import com.example.ticketing.common.response.PageResponse;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.reservation.dto.ReservationCancelRequest;
import com.example.ticketing.reservation.dto.ReservationCancelResponse;
import com.example.ticketing.reservation.dto.ReservationCreateRequest;
import com.example.ticketing.reservation.dto.ReservationCreateResponse;
import com.example.ticketing.reservation.dto.ReservationDetailResponse;
import com.example.ticketing.reservation.dto.ReservationSummaryResponse;
import com.example.ticketing.reservation.service.ReservationService;
import com.example.ticketing.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    @PostMapping
    public ApiResponse<ReservationCreateResponse> create(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ReservationCreateRequest request
    ) {
        return ApiResponse.success(
                "예매가 생성되었습니다. 제한 시간 안에 결제를 완료해주세요.",
                reservationService.createWithPessimisticLock(userDetails.getUserId(), request)
        );
    }

    @GetMapping("/{reservationId}")
    public ApiResponse<ReservationDetailResponse> getReservation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reservationId
    ) {
        return ApiResponse.success(reservationService.getReservation(userDetails.getUserId(), reservationId));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<ReservationSummaryResponse>> getMyReservations(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) ReservationStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ApiResponse.success(reservationService.getMyReservations(userDetails.getUserId(), status, pageable));
    }

    @PostMapping("/{reservationId}/cancel")
    public ApiResponse<ReservationCancelResponse> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long reservationId,
            @RequestBody(required = false) ReservationCancelRequest request
    ) {
        return ApiResponse.success("예매가 취소되었습니다.", reservationService.cancel(userDetails.getUserId(), reservationId));
    }
}
