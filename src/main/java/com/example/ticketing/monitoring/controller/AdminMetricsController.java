package com.example.ticketing.monitoring.controller;

import com.example.ticketing.common.response.ApiResponse;
import com.example.ticketing.monitoring.dto.ReservationMetricsResponse;
import com.example.ticketing.monitoring.service.ReservationMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/metrics")
public class AdminMetricsController {

    private final ReservationMetricsService reservationMetricsService;

    @GetMapping("/reservations")
    public ApiResponse<ReservationMetricsResponse> getReservationMetrics(@RequestParam Long concertId) {
        return ApiResponse.success(reservationMetricsService.getReservationMetrics(concertId));
    }
}
