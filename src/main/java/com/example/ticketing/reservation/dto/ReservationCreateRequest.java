package com.example.ticketing.reservation.dto;

import jakarta.validation.constraints.NotNull;

public record ReservationCreateRequest(
        @NotNull(message = "공연 ID는 필수입니다.")
        Long concertId,

        @NotNull(message = "좌석 ID는 필수입니다.")
        Long seatId,

        String entryToken
) {
}
