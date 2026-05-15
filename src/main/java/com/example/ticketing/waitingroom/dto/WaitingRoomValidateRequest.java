package com.example.ticketing.waitingroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WaitingRoomValidateRequest(
        @NotNull(message = "공연 ID는 필수입니다.")
        Long concertId,

        @NotBlank(message = "대기열 토큰은 필수입니다.")
        String waitingToken
) {
}
