package com.example.ticketing.concert.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ConcertCreateRequest(
        @NotBlank(message = "공연명은 필수입니다.")
        String title,

        @NotBlank(message = "공연장은 필수입니다.")
        String venue,

        @Future(message = "공연일시는 미래여야 합니다.")
        @NotNull(message = "공연일시는 필수입니다.")
        LocalDateTime concertDateTime,

        @NotNull(message = "예매 시작 시간은 필수입니다.")
        LocalDateTime reservationStartAt,

        @NotNull(message = "예매 종료 시간은 필수입니다.")
        LocalDateTime reservationEndAt,

        @Min(value = 1, message = "총 좌석 수는 1 이상이어야 합니다.")
        int totalSeatCount
) {
}
