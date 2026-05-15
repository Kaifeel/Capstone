package com.example.ticketing.reservation.dto;

import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.seat.domain.SeatStatus;

import java.time.LocalDateTime;

public record ReservationDetailResponse(
        Long reservationId,
        ReservationStatus status,
        LocalDateTime expiresAt,
        LocalDateTime reservedAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt,
        ConcertInfo concert,
        SeatInfo seat
) {

    public static ReservationDetailResponse from(Reservation reservation) {
        return new ReservationDetailResponse(
                reservation.getId(),
                reservation.getStatus(),
                reservation.getExpiresAt(),
                reservation.getReservedAt(),
                reservation.getConfirmedAt(),
                reservation.getCancelledAt(),
                new ConcertInfo(
                        reservation.getConcert().getId(),
                        reservation.getConcert().getTitle(),
                        reservation.getConcert().getVenue(),
                        reservation.getConcert().getConcertDateTime()
                ),
                new SeatInfo(
                        reservation.getSeat().getId(),
                        reservation.getSeat().getSection(),
                        reservation.getSeat().getRow(),
                        reservation.getSeat().getNumber(),
                        reservation.getSeat().getPrice(),
                        reservation.getSeat().getStatus()
                )
        );
    }

    public record ConcertInfo(Long concertId, String title, String venue, LocalDateTime concertDateTime) {
    }

    public record SeatInfo(Long seatId, String section, String row, int number, int price, SeatStatus status) {
    }
}
