package com.example.ticketing.waitingroom.dto;

import java.time.LocalDateTime;

public record WaitingRoomValidateResponse(
        Long concertId,
        String entryToken,
        LocalDateTime expiresAt
) {
}
