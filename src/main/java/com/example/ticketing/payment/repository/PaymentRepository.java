package com.example.ticketing.payment.repository;

import com.example.ticketing.payment.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReservation_Id(Long reservationId);
}
