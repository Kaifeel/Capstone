package com.example.ticketing.payment;

import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.concert.repository.ConcertRepository;
import com.example.ticketing.payment.domain.Payment;
import com.example.ticketing.payment.domain.PaymentStatus;
import com.example.ticketing.payment.dto.MockPaymentRequest;
import com.example.ticketing.payment.dto.MockPaymentResult;
import com.example.ticketing.payment.repository.PaymentRepository;
import com.example.ticketing.payment.service.PaymentService;
import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.reservation.dto.ReservationCreateRequest;
import com.example.ticketing.reservation.dto.ReservationCreateResponse;
import com.example.ticketing.reservation.repository.ReservationRepository;
import com.example.ticketing.reservation.service.ReservationExpirationService;
import com.example.ticketing.reservation.service.ReservationService;
import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.domain.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.domain.User;
import com.example.ticketing.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationExpirationService reservationExpirationService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mockPay_success_confirmsReservationAndReservesSeat() {
        Fixture fixture = createPendingReservation("pay-success@example.com", 1);

        paymentService.mockPay(
                fixture.user().getId(),
                new MockPaymentRequest(fixture.reservationId(), MockPaymentResult.SUCCESS)
        );

        Reservation reservation = reservationRepository.findById(fixture.reservationId()).orElseThrow();
        Seat seat = seatRepository.findById(fixture.seat().getId()).orElseThrow();
        Payment payment = paymentRepository.findByReservation_Id(fixture.reservationId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getApprovedAt()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(reservation.getConfirmedAt()).isNotNull();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.RESERVED);
        assertThat(seat.getHoldExpiresAt()).isNull();
    }

    @Test
    void mockPay_failed_cancelsReservationAndReleasesSeat() {
        Fixture fixture = createPendingReservation("pay-failed@example.com", 1);

        paymentService.mockPay(
                fixture.user().getId(),
                new MockPaymentRequest(fixture.reservationId(), MockPaymentResult.FAILED)
        );

        Reservation reservation = reservationRepository.findById(fixture.reservationId()).orElseThrow();
        Seat seat = seatRepository.findById(fixture.seat().getId()).orElseThrow();
        Payment payment = paymentRepository.findByReservation_Id(fixture.reservationId()).orElseThrow();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailedAt()).isNotNull();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.getCancelledAt()).isNotNull();
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seat.getHoldExpiresAt()).isNull();
    }

    @Test
    void mockPay_failsWhenDifferentUserRequestsPayment() {
        Fixture fixture = createPendingReservation("pay-owner@example.com", 1);
        User otherUser = saveUser("pay-other@example.com");

        assertThatThrownBy(() -> paymentService.mockPay(
                otherUser.getId(),
                new MockPaymentRequest(fixture.reservationId(), MockPaymentResult.SUCCESS)
        )).isInstanceOf(BusinessException.class);

        Reservation reservation = reservationRepository.findById(fixture.reservationId()).orElseThrow();
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    void expirePendingReservations_expiresReservationAndReleasesSeatAndMarksPaymentTimeout() {
        Fixture fixture = createPendingReservation("pay-timeout@example.com", 1);
        jdbcExpireReservation(fixture.reservationId());

        int expiredCount = reservationExpirationService.expirePendingReservations();

        Reservation reservation = reservationRepository.findById(fixture.reservationId()).orElseThrow();
        Seat seat = seatRepository.findById(fixture.seat().getId()).orElseThrow();
        Payment payment = paymentRepository.findByReservation_Id(fixture.reservationId()).orElseThrow();

        assertThat(expiredCount).isEqualTo(1);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(seat.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(seat.getHoldExpiresAt()).isNull();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.TIMEOUT);
        assertThat(payment.getFailedAt()).isNotNull();
    }

    private Fixture createPendingReservation(String email, int seatNumber) {
        User user = saveUser(email);
        Concert concert = saveReservableConcert();
        Seat seat = seatRepository.save(Seat.create(concert, "A", "1", seatNumber, 150000));
        ReservationCreateResponse response = reservationService.createWithPessimisticLock(
                user.getId(),
                new ReservationCreateRequest(concert.getId(), seat.getId(), null)
        );
        return new Fixture(user, concert, seat, response.reservationId());
    }

    private User saveUser(String email) {
        return userRepository.save(User.createUser(email, "encoded-password", "테스트 사용자"));
    }

    private Concert saveReservableConcert() {
        LocalDateTime now = LocalDateTime.now();
        return concertRepository.save(Concert.create(
                "결제 테스트 콘서트",
                "테스트 공연장",
                now.plusDays(10),
                now.minusMinutes(1),
                now.plusDays(1),
                100
        ));
    }

    private void jdbcExpireReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow();
        LocalDateTime expiredAt = LocalDateTime.now().minusMinutes(1);
        jdbcTemplate.update(
                "update reservations set expires_at = ?, updated_at = ? where id = ?",
                expiredAt,
                expiredAt,
                reservationId
        );
        jdbcTemplate.update(
                "update seats set hold_expires_at = ?, updated_at = ? where id = ?",
                expiredAt,
                expiredAt,
                reservation.getSeat().getId()
        );
    }

    private record Fixture(User user, Concert concert, Seat seat, Long reservationId) {
    }
}
