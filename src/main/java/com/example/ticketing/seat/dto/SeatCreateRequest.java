package com.example.ticketing.seat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record SeatCreateRequest(
        @NotBlank(message = "좌석 구역은 필수입니다.")
        String section,

        @NotBlank(message = "좌석 열은 필수입니다.")
        String row,

        @Min(value = 1, message = "좌석 번호는 1 이상이어야 합니다.")
        int number,

        @Min(value = 0, message = "가격은 0 이상이어야 합니다.")
        int price
) {
}
