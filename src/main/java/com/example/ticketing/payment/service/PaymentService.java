package com.example.ticketing.payment.service;

import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.payment.domain.Payment;
import com.example.ticketing.payment.dto.MockPaymentRequest;
import com.example.ticketing.payment.dto.MockPaymentResponse;
import com.example.ticketing.payment.dto.MockPaymentResult;
import com.example.ticketing.payment.repository.PaymentRepository;
import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final Clock clock;

    @Transactional
    public MockPaymentResponse mockPay(Long userId, MockPaymentRequest request) {
        LocalDateTime now = LocalDateTime.now(clock);
        Reservation reservation = reservationRepository.findById(request.reservationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        if (!reservation.isOwner(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        if (reservation.getExpiresAt().isBefore(now)) {
            expireReservation(reservation, now);
            throw new BusinessException(ErrorCode.RESERVATION_EXPIRED);
        }

        Payment payment = paymentRepository.findByReservation_Id(reservation.getId())
                .orElseGet(() -> paymentRepository.save(Payment.createPending(
                        reservation,
                        reservation.getSeat().getPrice(),
                        now
                )));

        if (request.result() == MockPaymentResult.SUCCESS) {
            payment.succeed(now);
            reservation.confirm(now);
            reservation.getSeat().reserve();
        } else {
            payment.fail(now);
            reservation.cancel(now);
            reservation.getSeat().release();
        }

        return MockPaymentResponse.from(payment);
    }

    @Transactional
    public void markTimeout(Reservation reservation, LocalDateTime now) {
        Payment payment = paymentRepository.findByReservation_Id(reservation.getId())
                .orElseGet(() -> paymentRepository.save(Payment.createPending(
                        reservation,
                        reservation.getSeat().getPrice(),
                        reservation.getReservedAt()
                )));
        payment.timeout(now);
    }

    private void expireReservation(Reservation reservation, LocalDateTime now) {
        reservation.expire(now);
        reservation.getSeat().release();
    }
}
