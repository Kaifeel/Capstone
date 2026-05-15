package com.example.ticketing.concert.dto;

import com.example.ticketing.concert.domain.Concert;

import java.time.LocalDateTime;

public record ConcertResponse(
        Long concertId,
        String title,
        String venue,
        LocalDateTime concertDateTime,
        LocalDateTime reservationStartAt,
        LocalDateTime reservationEndAt,
        int totalSeatCount
) {

    public static ConcertResponse from(Concert concert) {
        return new ConcertResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getVenue(),
                concert.getConcertDateTime(),
                concert.getReservationStartAt(),
                concert.getReservationEndAt(),
                concert.getTotalSeatCount()
        );
    }
}
