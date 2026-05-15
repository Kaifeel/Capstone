package com.example.ticketing.monitoring.dto;

public record ReservationMetricsResponse(
        Long concertId,
        long totalReservationCount,
        long pendingCount,
        long confirmedCount,
        long cancelledCount,
        long expiredCount,
        long availableSeatCount,
        long holdSeatCount,
        long reservedSeatCount,
        long waitingRoomLength
) {
}
