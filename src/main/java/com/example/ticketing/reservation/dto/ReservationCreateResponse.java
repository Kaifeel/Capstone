package com.example.ticketing.reservation.dto;

import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.seat.domain.SeatStatus;

import java.time.LocalDateTime;

public record ReservationCreateResponse(
        Long reservationId,
        Long concertId,
        Long seatId,
        ReservationStatus status,
        SeatStatus seatStatus,
        LocalDateTime expiresAt,
        int amount
) {

    public static ReservationCreateResponse from(Reservation reservation) {
        return new ReservationCreateResponse(
                reservation.getId(),
                reservation.getConcert().getId(),
                reservation.getSeat().getId(),
                reservation.getStatus(),
                reservation.getSeat().getStatus(),
                reservation.getExpiresAt(),
                reservation.getSeat().getPrice()
        );
    }
}
