package com.example.ticketing.seat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SeatBulkCreateRequest(
        @Valid
        @NotEmpty(message = "생성할 좌석은 1개 이상이어야 합니다.")
        List<SeatCreateRequest> seats
) {
}
