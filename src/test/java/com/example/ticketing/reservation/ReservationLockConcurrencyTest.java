package com.example.ticketing.reservation;

import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.concert.repository.ConcertRepository;
import com.example.ticketing.reservation.dto.ReservationCreateRequest;
import com.example.ticketing.reservation.service.ReservationService;
import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.domain.SeatStatus;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.domain.User;
import com.example.ticketing.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReservationLockConcurrencyTest {

    private static final int USER_COUNT = 20;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ConcertRepository concertRepository;

    @Autowired
    private SeatRepository seatRepository;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from reservations");
        jdbcTemplate.update("delete from seats");
        jdbcTemplate.update("delete from concerts");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void pessimisticLock_allowsOnlyOneReservationForSameSeat() throws Exception {
        ConcurrentReservationResult result = runConcurrentReservation(
                "pessimistic",
                reservationService::createWithPessimisticLock
        );

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(USER_COUNT - 1);
        assertThat(result.activeReservationCount()).isEqualTo(1);
        assertThat(result.seatStatus()).isEqualTo(SeatStatus.HOLD);
    }

    @Test
    void optimisticLock_allowsOnlyOneReservationForSameSeat() throws Exception {
        ConcurrentReservationResult result = runConcurrentReservation(
                "optimistic",
                reservationService::createWithOptimisticLock
        );

        assertThat(result.successCount()).isEqualTo(1);
        assertThat(result.failureCount()).isEqualTo(USER_COUNT - 1);
        assertThat(result.activeReservationCount()).isEqualTo(1);
        assertThat(result.seatStatus()).isEqualTo(SeatStatus.HOLD);
    }

    private ConcurrentReservationResult runConcurrentReservation(
            String emailPrefix,
            BiFunction<Long, ReservationCreateRequest, ?> reservationFunction
    ) throws Exception {
        Concert concert = saveReservableConcert();
        Seat seat = seatRepository.saveAndFlush(Seat.create(concert, "A", "1", 1, 150000));
        List<User> users = saveUsers(emailPrefix);
        CountDownLatch readyLatch = new CountDownLatch(USER_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(USER_COUNT);

        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (User user : users) {
                futures.add(executorService.submit(createTask(
                        user.getId(),
                        concert.getId(),
                        seat.getId(),
                        readyLatch,
                        startLatch,
                        reservationFunction
                )));
            }

            assertThat(readyLatch.await(3, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            int successCount = 0;
            int failureCount = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                } else {
                    failureCount++;
                }
            }

            Long activeReservationCount = jdbcTemplate.queryForObject(
                    """
                            select count(*)
                            from reservations
                            where seat_id = ?
                              and status in ('PENDING', 'CONFIRMED')
                            """,
                    Long.class,
                    seat.getId()
            );
            String seatStatus = jdbcTemplate.queryForObject(
                    "select status from seats where id = ?",
                    String.class,
                    seat.getId()
            );

            return new ConcurrentReservationResult(
                    successCount,
                    failureCount,
                    activeReservationCount == null ? 0 : activeReservationCount,
                    SeatStatus.valueOf(seatStatus)
            );
        } finally {
            executorService.shutdownNow();
        }
    }

    private Callable<Boolean> createTask(
            Long userId,
            Long concertId,
            Long seatId,
            CountDownLatch readyLatch,
            CountDownLatch startLatch,
            BiFunction<Long, ReservationCreateRequest, ?> reservationFunction
    ) {
        return () -> {
            readyLatch.countDown();
            startLatch.await(3, TimeUnit.SECONDS);
            try {
                reservationFunction.apply(userId, new ReservationCreateRequest(concertId, seatId, null));
                return true;
            } catch (RuntimeException exception) {
                return false;
            }
        };
    }

    private List<User> saveUsers(String prefix) {
        List<User> users = new ArrayList<>();
        for (int i = 0; i < USER_COUNT; i++) {
            users.add(userRepository.save(User.createUser(
                    "%s-user%d@example.com".formatted(prefix, i),
                    "encoded-password",
                    "테스트 사용자"
            )));
        }
        userRepository.flush();
        return users;
    }

    private Concert saveReservableConcert() {
        LocalDateTime now = LocalDateTime.now();
        return concertRepository.saveAndFlush(Concert.create(
                "락 테스트 콘서트",
                "테스트 공연장",
                now.plusDays(10),
                now.minusMinutes(1),
                now.plusDays(1),
                100
        ));
    }

    private record ConcurrentReservationResult(
            int successCount,
            int failureCount,
            long activeReservationCount,
            SeatStatus seatStatus
    ) {
    }
}
