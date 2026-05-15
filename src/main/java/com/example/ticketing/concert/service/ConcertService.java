package com.example.ticketing.concert.service;

import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.response.PageResponse;
import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.concert.dto.ConcertCreateRequest;
import com.example.ticketing.concert.dto.ConcertDetailResponse;
import com.example.ticketing.concert.dto.ConcertResponse;
import com.example.ticketing.concert.dto.ConcertUpdateRequest;
import com.example.ticketing.concert.repository.ConcertRepository;
import com.example.ticketing.seat.domain.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public ConcertResponse create(ConcertCreateRequest request) {
        Concert concert = Concert.create(
                request.title(),
                request.venue(),
                request.concertDateTime(),
                request.reservationStartAt(),
                request.reservationEndAt(),
                request.totalSeatCount()
        );
        return ConcertResponse.from(concertRepository.save(concert));
    }

    public PageResponse<ConcertResponse> getConcerts(Pageable pageable) {
        return PageResponse.from(concertRepository.findByDeletedFalse(pageable).map(ConcertResponse::from));
    }

    public ConcertDetailResponse getConcert(Long concertId) {
        Concert concert = getActiveConcert(concertId);
        long availableCount = seatRepository.countByConcert_IdAndStatus(concertId, SeatStatus.AVAILABLE);
        long holdCount = seatRepository.countByConcert_IdAndStatus(concertId, SeatStatus.HOLD);
        long reservedCount = seatRepository.countByConcert_IdAndStatus(concertId, SeatStatus.RESERVED);
        return ConcertDetailResponse.of(concert, availableCount, holdCount, reservedCount);
    }

    @Transactional
    public ConcertResponse update(Long concertId, ConcertUpdateRequest request) {
        Concert concert = getActiveConcert(concertId);
        concert.update(
                request.title(),
                request.venue(),
                request.concertDateTime(),
                request.reservationStartAt(),
                request.reservationEndAt()
        );
        return ConcertResponse.from(concert);
    }

    @Transactional
    public Long delete(Long concertId) {
        Concert concert = getActiveConcert(concertId);
        concert.delete();
        return concert.getId();
    }

    public Concert getActiveConcert(Long concertId) {
        return concertRepository.findByIdAndDeletedFalse(concertId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCERT_NOT_FOUND));
    }
}
