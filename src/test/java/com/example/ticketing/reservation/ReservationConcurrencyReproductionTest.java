package com.example.ticketing.reservation;

import com.example.ticketing.concert.domain.Concert;
import com.example.ticketing.concert.repository.ConcertRepository;
import com.example.ticketing.seat.domain.Seat;
import com.example.ticketing.seat.repository.SeatRepository;
import com.example.ticketing.user.domain.User;
import com.example.ticketing.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReservationConcurrencyReproductionTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

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
    void naiveTransaction_canCreateDuplicatedReservationsWhenRequestsReadAvailableSeatAtSameTime() throws Exception {
        Concert concert = saveReservableConcert();
        Seat seat = seatRepository.saveAndFlush(Seat.create(concert, "A", "1", 1, 150000));
        List<User> users = List.of(
                saveUser("concurrency-user1@example.com"),
                saveUser("concurrency-user2@example.com")
        );

        CountDownLatch readCompletedLatch = new CountDownLatch(users.size());
        ExecutorService executorService = Executors.newFixedThreadPool(users.size());

        try {
            List<Callable<Boolean>> tasks = users.stream()
                    .map(user -> (Callable<Boolean>) () -> naiveReserve(user.getId(), concert.getId(), seat.getId(), readCompletedLatch))
                    .toList();

            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> task : tasks) {
                futures.add(executorService.submit(task));
            }

            List<Boolean> results = new ArrayList<>();
            for (Future<Boolean> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
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

            assertThat(results).containsExactlyInAnyOrder(true, true);
            assertThat(activeReservationCount).isEqualTo(2);
        } finally {
            executorService.shutdownNow();
        }
    }

    private Boolean naiveReserve(Long userId, Long concertId, Long seatId, CountDownLatch readCompletedLatch) {
        return transactionTemplate.execute(status -> {
            String seatStatus = jdbcTemplate.queryForObject(
                    "select status from seats where id = ?",
                    String.class,
                    seatId
            );
            Long activeReservationCount = jdbcTemplate.queryForObject(
                    """
                            select count(*)
                            from reservations
                            where seat_id = ?
                              and status in ('PENDING', 'CONFIRMED')
                            """,
                    Long.class,
                    seatId
            );

            readCompletedLatch.countDown();
            await(readCompletedLatch);

            if (!"AVAILABLE".equals(seatStatus) || activeReservationCount == null || activeReservationCount > 0) {
                return false;
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiresAt = now.plusMinutes(5);
            jdbcTemplate.update(
                    "update seats set status = 'HOLD', hold_expires_at = ?, updated_at = ? where id = ?",
                    expiresAt,
                    now,
                    seatId
            );
            jdbcTemplate.update(
                    """
                            insert into reservations
                                (user_id, concert_id, seat_id, status, expires_at, reserved_at, created_at, updated_at)
                            values
                                (?, ?, ?, 'PENDING', ?, ?, ?, ?)
                            """,
                    userId,
                    concertId,
                    seatId,
                    expiresAt,
                    now,
                    now,
                    now
            );
            return true;
        });
    }

    private void await(CountDownLatch latch) {
        try {
            boolean completed = latch.await(3, TimeUnit.SECONDS);
            if (!completed) {
                throw new IllegalStateException("동시성 테스트 준비 시간이 초과되었습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private User saveUser(String email) {
        return userRepository.saveAndFlush(User.createUser(email, "encoded-password", "테스트 사용자"));
    }

    private Concert saveReservableConcert() {
        LocalDateTime now = LocalDateTime.now();
        return concertRepository.saveAndFlush(Concert.create(
                "동시성 테스트 콘서트",
                "테스트 공연장",
                now.plusDays(10),
                now.minusMinutes(1),
                now.plusDays(1),
                100
        ));
    }
}
