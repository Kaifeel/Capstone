package com.example.ticketing.seat.dto;

public record SeatBulkCreateResponse(
        Long concertId,
        int createdCount
) {
}
