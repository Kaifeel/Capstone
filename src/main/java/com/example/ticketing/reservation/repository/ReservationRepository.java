package com.example.ticketing.reservation.repository;

import com.example.ticketing.reservation.domain.Reservation;
import com.example.ticketing.reservation.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsBySeat_IdAndStatusIn(Long seatId, Collection<ReservationStatus> statuses);

    boolean existsByUser_IdAndSeat_IdAndStatusIn(Long userId, Long seatId, Collection<ReservationStatus> statuses);

    Page<Reservation> findByUser_Id(Long userId, Pageable pageable);

    Page<Reservation> findByUser_IdAndStatus(Long userId, ReservationStatus status, Pageable pageable);

    long countByConcert_Id(Long concertId);

    long countByConcert_IdAndStatus(Long concertId, ReservationStatus status);

    @Query("""
            select r
            from Reservation r
            join fetch r.seat
            join fetch r.concert
            where r.status = :status
              and r.expiresAt < :now
            """)
    List<Reservation> findExpiredReservations(@Param("status") ReservationStatus status, @Param("now") LocalDateTime now);
}
