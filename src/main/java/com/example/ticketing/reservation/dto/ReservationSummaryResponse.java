package com.example.ticketing.reservation.dto;

import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;

import java.time.LocalDateTime;

public record ReservationSummaryResponse(
        Long reservationId,
        ReservationStatus status,
        String concertTitle,
        LocalDateTime concertDateTime,
        String seatLabel,
        int amount,
        LocalDateTime reservedAt,
        LocalDateTime confirmedAt
) {

    public static ReservationSummaryResponse from(Reservation reservation) {
        String seatLabel = "%s구역 %s열 %d번".formatted(
                reservation.getSeat().getSection(),
                reservation.getSeat().getRow(),
                reservation.getSeat().getNumber()
        );
        return new ReservationSummaryResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getConcert().getTitle(),
                reservation.getConcert().getConcertDateTime(),
                seatLabel,
                reservation.getSeat().getPrice(),
                reservation.getReservedAt(),
                reservation.getConfirmedAt()
        );
    }
}
