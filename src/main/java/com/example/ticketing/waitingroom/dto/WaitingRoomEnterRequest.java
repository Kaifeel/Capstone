package com.example.ticketing.waitingroom.dto;

import jakarta.validation.constraints.NotNull;

public record WaitingRoomEnterRequest(
        @NotNull(message = "공연 ID는 필수입니다.")
        Long concertId
) {
}
