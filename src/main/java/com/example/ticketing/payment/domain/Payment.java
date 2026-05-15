package com.example.ticketing.payment.domain;

import com.example.ticketing.common.entity.BaseEntity;
import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.reservation.domain.Reservation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    private LocalDateTime failedAt;

    private Payment(Reservation reservation, int amount, LocalDateTime requestedAt) {
        this.reservation = reservation;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.requestedAt = requestedAt;
    }

    public static Payment createPending(Reservation reservation, int amount, LocalDateTime requestedAt) {
        if (amount < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return new Payment(reservation, amount, requestedAt);
    }

    public void succeed(LocalDateTime now) {
        if (status != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = PaymentStatus.SUCCESS;
        this.approvedAt = now;
    }

    public void fail(LocalDateTime now) {
        if (status != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = PaymentStatus.FAILED;
        this.failedAt = now;
    }

    public void timeout(LocalDateTime now) {
        if (status != PaymentStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = PaymentStatus.TIMEOUT;
        this.failedAt = now;
    }
}
