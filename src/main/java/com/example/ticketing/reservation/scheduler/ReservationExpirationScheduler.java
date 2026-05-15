package com.example.ticketing.reservation.scheduler;

import com.example.ticketing.reservation.service.ReservationExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReservationExpirationScheduler {

    private final ReservationExpirationService reservationExpirationService;

    @Scheduled(fixedDelayString = "${reservation.expiration-scheduler.fixed-delay-ms:30000}")
    public void expirePendingReservations() {
        reservationExpirationService.expirePendingReservations();
    }
}
