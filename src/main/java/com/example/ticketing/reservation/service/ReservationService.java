package com.example.ticketing.reservation.service;

import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.common.response.PageResponse;
import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.concert.service.ConcertService;
import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.reservation.dto.ReservationCancelResponse;
import com.example.ticketing.reservation.dto.ReservationCreateRequest;
import com.example.ticketing.reservation.dto.ReservationCreateResponse;
import com.example.ticketing.reservation.dto.ReservationDetailResponse;
import com.example.ticketing.reservation.dto.ReservationSummaryResponse;
import com.example.ticketing.reservation.repository.ReservationRepository;
import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.domain.User;
import com.example.ticketing.user.service.UserService;
import com.example.ticketing.waitingroom.service.WaitingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

    private static final long HOLD_MINUTES = 5;
    private static final List<ReservationStatus> ACTIVE_STATUSES = List.of(
            ReservationStatus.PENDING,
            ReservationStatus.CONFIRMED
    );

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final ConcertService concertService;
    private final UserService userService;
    private final WaitingRoomService waitingRoomService;
    private final Clock clock;

    @Transactional
    public ReservationCreateResponse create(Long userId, ReservationCreateRequest request) {
        return createInternal(userId, request, seatRepository::findById);
    }

    @Transactional
    public ReservationCreateResponse createWithPessimisticLock(Long userId, ReservationCreateRequest request) {
        return createInternal(userId, request, seatRepository::findByIdForUpdate);
    }

    @Transactional
    public ReservationCreateResponse createWithOptimisticLock(Long userId, ReservationCreateRequest request) {
        try {
            return createInternal(userId, request, seatRepository::findById);
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new BusinessException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
    }

    private ReservationCreateResponse createInternal(
            Long userId,
            ReservationCreateRequest request,
            Function<Long, java.util.Optional<Seat>> seatLoader
    ) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = userService.getById(userId);
        Concert concert = concertService.getActiveConcert(request.concertId());
        concert.validateReservable(now);
        waitingRoomService.validateEntryTokenIfPresent(userId, request.concertId(), request.entryToken());

        Seat seat = seatLoader.apply(request.seatId())
                .orElseThrow(() -> new BusinessException(ErrorCode.SEAT_NOT_FOUND));

        if (!seat.getConcert().getId().equals(concert.getId())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (reservationRepository.existsBySeat_IdAndStatusIn(seat.getId(), ACTIVE_STATUSES)) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }

        if (reservationRepository.existsByUser_IdAndSeat_IdAndStatusIn(userId, seat.getId(), ACTIVE_STATUSES)) {
            throw new BusinessException(ErrorCode.ACTIVE_RESERVATION_ALREADY_EXISTS);
        }

        LocalDateTime expiresAt = now.plusMinutes(HOLD_MINUTES);
        seat.hold(expiresAt);
        Reservation reservation = Reservation.createPending(user, concert, seat, now, expiresAt);
        Reservation savedReservation = reservationRepository.saveAndFlush(reservation);
        seatRepository.flush();
        return ReservationCreateResponse.from(savedReservation);
    }

    public ReservationDetailResponse getReservation(Long userId, Long reservationId) {
        Reservation reservation = getOwnedReservation(userId, reservationId);
        return ReservationDetailResponse.from(reservation);
    }

    public PageResponse<ReservationSummaryResponse> getMyReservations(Long userId, ReservationStatus status, Pageable pageable) {
        if (status == null) {
            return PageResponse.from(reservationRepository.findByUser_Id(userId, pageable).map(ReservationSummaryResponse::from));
        }
        return PageResponse.from(reservationRepository.findByUser_IdAndStatus(userId, status, pageable).map(ReservationSummaryResponse::from));
    }

    @Transactional
    public ReservationCancelResponse cancel(Long userId, Long reservationId) {
        Reservation reservation = getOwnedReservation(userId, reservationId);
        LocalDateTime now = LocalDateTime.now(clock);
        reservation.cancel(now);
        reservation.getSeat().release();
        return ReservationCancelResponse.from(reservation);
    }

    private Reservation getOwnedReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        if (!reservation.isOwner(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return reservation;
    }
}
