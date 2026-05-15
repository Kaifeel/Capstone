package com.example.ticketing.seat.dto;

import java.util.List;

public record SeatListResponse(
        Long concertId,
        List<SeatResponse> seats
) {
}
