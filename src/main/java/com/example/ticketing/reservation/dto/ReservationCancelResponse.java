package com.example.ticketing.reservation.dto;

import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.seat.domain.SeatStatus;

import java.time.LocalDateTime;

public record ReservationCancelResponse(
        Long reservationId,
        ReservationStatus reservationStatus,
        Long seatId,
        SeatStatus seatStatus,
        LocalDateTime cancelledAt
) {

    public static ReservationCancelResponse from(Reservation reservation) {
        return new ReservationCancelResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getSeat().getId(),
                reservation.getSeat().getStatus(),
                reservation.getCancelledAt()
        );
    }
}
