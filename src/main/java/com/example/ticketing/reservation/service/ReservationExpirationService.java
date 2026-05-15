package com.example.ticketing.reservation.service;

import com.example.ticketing.payment.service.PaymentService;
import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.reservation.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationExpirationService {

    private final ReservationRepository reservationRepository;
    private final PaymentService paymentService;
    private final Clock clock;

    @Transactional
    public int expirePendingReservations() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(ReservationStatus.PENDING, now);

        for (Reservation reservation : expiredReservations) {
            reservation.expire(now);
            reservation.getSeat().release();
            paymentService.markTimeout(reservation, now);
        }

        return expiredReservations.size();
    }
}
