package com.example.ticketing.concert.dto;

import com.example.ticketing.concert.domain.Concert;

import java.time.LocalDateTime;

public record ConcertDetailResponse(
        Long concertId,
        String title,
        String venue,
        LocalDateTime concertDateTime,
        LocalDateTime reservationStartAt,
        LocalDateTime reservationEndAt,
        int totalSeatCount,
        long availableSeatCount,
        long holdSeatCount,
        long reservedSeatCount
) {

    public static ConcertDetailResponse of(Concert concert, long availableSeatCount, long holdSeatCount, long reservedSeatCount) {
        return new ConcertDetailResponse(
                concert.getId(),
                concert.getTitle(),
                concert.getVenue(),
                concert.getConcertDateTime(),
                concert.getReservationStartAt(),
                concert.getReservationEndAt(),
                concert.getTotalSeatCount(),
                availableSeatCount,
                holdSeatCount,
                reservedSeatCount
        );
    }
}
