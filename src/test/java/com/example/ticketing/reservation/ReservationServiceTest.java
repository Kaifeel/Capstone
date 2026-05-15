package com.example.ticketing.reservation;

import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.concert.repository.ConcertRepository;
import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.reservation.dto.ReservationCancelResponse;
import com.example.ticketing.reservation.dto.ReservationCreateRequest;
import com.example.ticketing.reservation.dto.ReservationCreateResponse;
import com.example.ticketing.reservation.repository.ReservationRepository;
import com.example.ticketing.reservation.service.ReservationService;
import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.domain.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.domain.User;
import com.example.ticketing.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    void create_holdsSeatAndCreatesPendingReservationInSingleTransaction() {
        User user = saveUser("user1@example.com");
        Concert concert = saveReservableConcert();
        Seat seat = saveSeat(concert, 1);

        ReservationCreateResponse response = reservationService.create(
                user.getId(),
                new ReservationCreateRequest(concert.getId(), seat.getId(), null)
        );

        Reservation reservation = reservationRepository.findById(response.reservationId()).orElseThrow();
        Seat heldSeat = seatRepository.findById(seat.getId()).orElseThrow();

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
        assertThat(reservation.getUser().getId()).isEqualTo(user.getId());
        assertThat(reservation.getConcert().getId()).isEqualTo(concert.getId());
        assertThat(reservation.getSeat().getId()).isEqualTo(seat.getId());
        assertThat(heldSeat.getStatus()).isEqualTo(SeatStatus.HOLD);
        assertThat(heldSeat.getHoldExpiresAt()).isEqualTo(reservation.getExpiresAt());
        assertThat(reservation.getExpiresAt()).isAfter(reservation.getReservedAt());
    }

    @Test
    void create_failsWhenSeatAlreadyHasActiveReservation() {
        User user1 = saveUser("user2@example.com");
        User user2 = saveUser("user3@example.com");
        Concert concert = saveReservableConcert();
        Seat seat = saveSeat(concert, 1);

        reservationService.create(user1.getId(), new ReservationCreateRequest(concert.getId(), seat.getId(), null));

        assertThatThrownBy(() ->
                reservationService.create(user2.getId(), new ReservationCreateRequest(concert.getId(), seat.getId(), null))
        ).isInstanceOf(BusinessException.class);

        long activeCount = reservationRepository.count();
        Seat heldSeat = seatRepository.findById(seat.getId()).orElseThrow();

        assertThat(activeCount).isEqualTo(1);
        assertThat(heldSeat.getStatus()).isEqualTo(SeatStatus.HOLD);
    }

    @Test
    void cancel_cancelsReservationAndReleasesSeatInSingleTransaction() {
        User user = saveUser("user4@example.com");
        Concert concert = saveReservableConcert();
        Seat seat = saveSeat(concert, 1);
        ReservationCreateResponse createResponse = reservationService.create(
                user.getId(),
                new ReservationCreateRequest(concert.getId(), seat.getId(), null)
        );

        ReservationCancelResponse cancelResponse = reservationService.cancel(user.getId(), createResponse.reservationId());

        Reservation reservation = reservationRepository.findById(createResponse.reservationId()).orElseThrow();
        Seat releasedSeat = seatRepository.findById(seat.getId()).orElseThrow();

        assertThat(cancelResponse.reservationStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledAt()).isNotNull();
        assertThat(releasedSeat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(releasedSeat.getHoldExpiresAt()).isNull();
    }

    @Test
    void create_failsWhenSeatBelongsToDifferentConcert() {
        User user = saveUser("user5@example.com");
        Concert requestedConcert = saveReservableConcert();
        Concert seatConcert = saveReservableConcert();
        Seat seat = saveSeat(seatConcert, 1);

        assertThatThrownBy(() ->
                reservationService.create(user.getId(), new ReservationCreateRequest(requestedConcert.getId(), seat.getId(), null))
        ).isInstanceOf(BusinessException.class);

        assertThat(reservationRepository.count()).isZero();
        assertThat(seatRepository.findById(seat.getId()).orElseThrow().getStatus()).isEqualTo(SeatStatus.AVAILABLE);
    }

    private User saveUser(String email) {
        return userRepository.save(User.createUser(email, "encoded-password", "테스트 사용자"));
    }

    private Concert saveReservableConcert() {
        LocalDateTime now = LocalDateTime.now();
        return concertRepository.save(Concert.create(
                "테스트 콘서트",
                "테스트 공연장",
                now.plusDays(10),
                now.minusMinutes(1),
                now.plusDays(1),
                100
        ));
    }

    private Seat saveSeat(Concert concert, int number) {
        return seatRepository.save(Seat.create(concert, "A", "1", number, 150000));
    }
}
