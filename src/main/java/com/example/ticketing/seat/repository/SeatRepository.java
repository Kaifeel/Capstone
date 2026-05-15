package com.example.ticketing.seat.repository;

import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.domain.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByConcert_Id(Long concertId);

    List<Seat> findByConcert_IdAndStatus(Long concertId, SeatStatus status);

    long countByConcert_IdAndStatus(Long concertId, SeatStatus status);

    boolean existsByConcert_IdAndSectionAndRowAndNumber(Long concertId, String section, String row, int number);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :seatId")
    Optional<Seat> findByIdForUpdate(@Param("seatId") Long seatId);

    List<Seat> findByStatusAndHoldExpiresAtBefore(SeatStatus status, LocalDateTime now);
}
