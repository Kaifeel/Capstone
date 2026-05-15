package com.example.ticketing.concert.domain;

import com.example.ticketing.common.entity.BaseEntity;
import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "concerts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Concert extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 200)
    private String venue;

    @Column(nullable = false)
    private LocalDateTime concertDateTime;

    @Column(nullable = false)
    private LocalDateTime reservationStartAt;

    @Column(nullable = false)
    private LocalDateTime reservationEndAt;

    @Column(nullable = false)
    private int totalSeatCount;

    @Column(nullable = false)
    private boolean deleted;

    private Concert(String title, String venue, LocalDateTime concertDateTime,
                    LocalDateTime reservationStartAt, LocalDateTime reservationEndAt, int totalSeatCount) {
        validateReservationPeriod(reservationStartAt, reservationEndAt);
        if (totalSeatCount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        this.title = title;
        this.venue = venue;
        this.concertDateTime = concertDateTime;
        this.reservationStartAt = reservationStartAt;
        this.reservationEndAt = reservationEndAt;
        this.totalSeatCount = totalSeatCount;
        this.deleted = false;
    }

    public static Concert create(String title, String venue, LocalDateTime concertDateTime,
                                 LocalDateTime reservationStartAt, LocalDateTime reservationEndAt, int totalSeatCount) {
        return new Concert(title, venue, concertDateTime, reservationStartAt, reservationEndAt, totalSeatCount);
    }

    public void update(String title, String venue, LocalDateTime concertDateTime,
                       LocalDateTime reservationStartAt, LocalDateTime reservationEndAt) {
        validateReservationPeriod(reservationStartAt, reservationEndAt);
        this.title = title;
        this.venue = venue;
        this.concertDateTime = concertDateTime;
        this.reservationStartAt = reservationStartAt;
        this.reservationEndAt = reservationEndAt;
    }

    public void delete() {
        this.deleted = true;
    }

    public void validateReservable(LocalDateTime now) {
        if (deleted || now.isBefore(reservationStartAt) || now.isAfter(reservationEndAt)) {
            throw new BusinessException(ErrorCode.INVALID_STATUS);
        }
    }

    private void validateReservationPeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (!startAt.isBefore(endAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }
}
