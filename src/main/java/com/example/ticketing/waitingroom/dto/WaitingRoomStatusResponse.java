package com.example.ticketing.waitingroom.dto;

import java.time.LocalDateTime;

public record WaitingRoomStatusResponse(
        Long concertId,
        String waitingToken,
        long rank,
        long waitingCount,
        boolean allowed,
        WaitingTokenStatus status,
        long estimatedWaitSeconds,
        LocalDateTime expiresAt
) {
}
