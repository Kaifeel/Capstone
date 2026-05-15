package com.example.ticketing.payment.dto;

import com.example.ticketing.payment.domain.Payment;
import com.example.ticketing.payment.domain.PaymentStatus;
import com.example.ticketing.reservation.domain.ReservationStatus;
import com.example.ticketing.seat.domain.SeatStatus;

import java.time.LocalDateTime;

public record MockPaymentResponse(
        Long paymentId,
        Long reservationId,
        PaymentStatus paymentStatus,
        ReservationStatus reservationStatus,
        SeatStatus seatStatus,
        LocalDateTime approvedAt,
        LocalDateTime failedAt
) {

    public static MockPaymentResponse from(Payment payment) {
        return new MockPaymentResponse(
                payment.getId(),
                payment.getReservation().getId(),
                payment.getStatus(),
                payment.getReservation().getStatus(),
                payment.getReservation().getSeat().getStatus(),
                payment.getApprovedAt(),
                payment.getFailedAt()
        );
    }
}
