package com.example.ticketing.seat.domain;

import com.example.ticketing.common.entity.BaseEntity;
import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.concert.domain.Concert;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_seats_position",
                columnNames = {"concert_id", "section", "seat_row", "seat_number"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert;

    @Column(nullable = false, length = 50)
    private String section;

    @Column(name = "seat_row", nullable = false, length = 20)
    private String row;

    @Column(name = "seat_number", nullable = false)
    private int number;

    @Column(nullable = false)
    private int price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SeatStatus status;

    @Version
    private Long version;

    private LocalDateTime holdExpiresAt;

    private Seat(Concert concert, String section, String row, int number, int price) {
        if (number <= 0 || price < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.concert = concert;
        this.section = section;
        this.row = row;
        this.number = number;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    public static Seat create(Concert concert, String section, String row, int number, int price) {
        return new Seat(concert, section, row, number, price);
    }

    public void hold(LocalDateTime expiresAt) {
        if (status != SeatStatus.AVAILABLE) {
            throw new BusinessException(ErrorCode.SEAT_ALREADY_OCCUPIED);
        }
        this.status = SeatStatus.HOLD;
        this.holdExpiresAt = expiresAt;
    }

    public void reserve() {
        if (status != SeatStatus.HOLD) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = SeatStatus.RESERVED;
        this.holdExpiresAt = null;
    }

    public void release() {
        if (status == SeatStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
        this.status = SeatStatus.AVAILABLE;
        this.holdExpiresAt = null;
    }

    public void cancel() {
        this.status = SeatStatus.CANCELLED;
        this.holdExpiresAt = null;
    }
}
