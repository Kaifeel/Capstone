package com.example.ticketing.seat.service;

import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.concert.service.ConcertService;
import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.domain.SeatStatus;
import com.example.ticketing.seat.dto.SeatBulkCreateRequest;
import com.example.ticketing.seat.dto.SeatBulkCreateResponse;
import com.example.ticketing.seat.dto.SeatCreateRequest;
import com.example.ticketing.seat.dto.SeatListResponse;
import com.example.ticketing.seat.dto.SeatResponse;
import com.example.ticketing.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatService {

    private final SeatRepository seatRepository;
    private final ConcertService concertService;

    @Transactional
    public SeatBulkCreateResponse createSeats(Long concertId, SeatBulkCreateRequest request) {
        Concert concert = concertService.getActiveConcert(concertId);
        Set<String> requestedPositions = new HashSet<>();

        for (SeatCreateRequest seatRequest : request.seats()) {
            String positionKey = "%s:%s:%d".formatted(seatRequest.section(), seatRequest.row(), seatRequest.number());
            if (!requestedPositions.add(positionKey)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }

            if (seatRepository.existsByConcert_IdAndSectionAndRowAndNumber(
                    concertId,
                    seatRequest.section(),
                    seatRequest.row(),
                    seatRequest.number()
            )) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        List<Seat> seats = request.seats()
                .stream()
                .map(seat -> Seat.create(concert, seat.section(), seat.row(), seat.number(), seat.price()))
                .toList();
        seatRepository.saveAll(seats);
        return new SeatBulkCreateResponse(concertId, seats.size());
    }

    public SeatListResponse getSeats(Long concertId, SeatStatus status) {
        concertService.getActiveConcert(concertId);
        List<Seat> seats = status == null
                ? seatRepository.findByConcert_Id(concertId)
                : seatRepository.findByConcert_IdAndStatus(concertId, status);
        return new SeatListResponse(concertId, seats.stream().map(SeatResponse::from).toList());
    }

    public SeatResponse getSeat(Long seatId) {
        return SeatResponse.from(getById(seatId));
    }

    public Seat getById(Long seatId) {
        return seatRepository.findById(seatId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));
    }
}
