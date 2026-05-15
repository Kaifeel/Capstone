package com.example.ticketing.reservation.domain;

import com.example.ticketing.common.entity.BaseEntity;
import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private LocalDateTime reservedAt;

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;

    private Reservation(User user, Concert concert, Seat seat, LocalDateTime now, LocalDateTime expiresAt) {
        this.user = user;
        this.concert = concert;
        this.seat = seat;
        this.status = ReservationStatus.PENDING;
        this.reservedAt = now;
        this.expiresAt = expiresAt;
    }

    public static Reservation createPending(User user, Concert concert, Seat seat, LocalDateTime now, LocalDateTime expiresAt) {
        return new Reservation(user, concert, seat, now, expiresAt);
    }

    public void confirm(LocalDateTime now) {
        if (status != ReservationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = now;
    }

    public void cancel(LocalDateTime now) {
        if (status == ReservationStatus.CANCELLED || status == ReservationStatus.EXPIRED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = now;
    }

    public void expire(LocalDateTime now) {
        if (status != ReservationStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = ReservationStatus.EXPIRED;
        this.cancelledAt = now;
    }

    public boolean isOwner(Long userId) {
        return user.getId().equals(userId);
    }
}
