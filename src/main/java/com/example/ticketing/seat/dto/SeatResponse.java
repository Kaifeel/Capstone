package com.example.ticketing.seat.dto;

import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.domain.SeatStatus;

import java.time.LocalDateTime;

public record SeatResponse(
        Long seatId,
        Long concertId,
        String section,
        String row,
        int number,
        int price,
        SeatStatus status,
        LocalDateTime holdExpiresAt
) {

    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getConcert().getId(),
                seat.getSection(),
                seat.getRow(),
                seat.getNumber(),
                seat.getPrice(),
                seat.getStatus(),
                seat.getHoldExpiresAt()
        );
    }
}
