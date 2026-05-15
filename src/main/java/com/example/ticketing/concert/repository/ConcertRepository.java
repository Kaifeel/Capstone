package com.example.ticketing.concert.repository;

import com.example.ticketing.concert.domain.Concert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConcertRepository extends JpaRepository<Concert, Long> {

    Page<Concert> findByDeletedFalse(Pageable pageable);

    Optional<Concert> findByIdAndDeletedFalse(Long id);
}
