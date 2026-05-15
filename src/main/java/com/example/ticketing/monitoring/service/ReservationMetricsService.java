package com.example.ticketing.monitoring.service;

import com.example.ticketing.concert.service.ConcertService;
import com.example.ticketing.monitoring.dto.ReservationMetricsResponse;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.reservation.repository.ReservationRepository;
import com.example.ticketing.seat.domain.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.waitingroom.redis.WaitingRoomRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationMetricsService {

    private final ConcertService concertService;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final WaitingRoomRedisRepository waitingRoomRedisRepository;

    public ReservationMetricsResponse getReservationMetrics(Long concertId) {
        concertService.getActiveConcert(concertId);

        return new ReservationMetricsResponse(
                concertId,
                reservationRepository.countByConcert_Id(concertId),
                reservationRepository.countByConcert_IdAndStatus(concertId, ReservationStatus.PENDING),
                reservationRepository.countByConcert_IdAndStatus(concertId, ReservationStatus.CONFIRMED),
                reservationRepository.countByConcert_IdAndStatus(concertId, ReservationStatus.CANCELLED),
                reservationRepository.countByConcert_IdAndStatus(concertId, ReservationStatus.EXPIRED),
                seatRepository.countByConcert_IdAndStatus(concertId, SeatStatus.AVAILABLE),
                seatRepository.countByConcert_IdAndStatus(concertId, SeatStatus.HOLD),
                seatRepository.countByConcert_IdAndStatus(concertId, SeatStatus.RESERVED),
                getWaitingRoomLength(concertId)
        );
    }

    private long getWaitingRoomLength(Long concertId) {
        try {
            return waitingRoomRedisRepository.queueLength(concertId);
        } catch (RuntimeException exception) {
            return -1;
        }
    }
}
