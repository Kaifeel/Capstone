package com.example.ticketing.waitingroom.dto;

public record WaitingRoomEnterResponse(
        Long concertId,
        String waitingToken,
        long rank,
        long estimatedWaitSeconds,
        WaitingTokenStatus status
) {
}
