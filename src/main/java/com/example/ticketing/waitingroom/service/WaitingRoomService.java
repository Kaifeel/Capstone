package com.example.ticketing.waitingroom.service;

import com.example.ticketing.common.exception.BusinessException;
import com.example.ticketing.common.exception.ErrorCode;
import com.example.ticketing.concert.service.ConcertService;
import com.example.ticketing.waitingroom.dto.WaitingRoomEnterRequest;
import com.example.ticketing.waitingroom.dto.WaitingRoomEnterResponse;
import com.example.ticketing.waitingroom.dto.WaitingRoomStatusResponse;
import com.example.ticketing.waitingroom.dto.WaitingRoomValidateRequest;
import com.example.ticketing.waitingroom.dto.WaitingRoomValidateResponse;
import com.example.ticketing.waitingroom.dto.WaitingTokenStatus;
import com.example.ticketing.waitingroom.redis.WaitingRoomRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WaitingRoomService {

    private static final Duration USER_TOKEN_TTL = Duration.ofHours(6);
    private static final Duration ENTRY_TOKEN_TTL = Duration.ofMinutes(5);
    private static final long DEFAULT_ADMISSION_LIMIT = 100;
    private static final long ESTIMATED_SECONDS_PER_100_USERS = 10;

    private final WaitingRoomRedisRepository waitingRoomRedisRepository;
    private final ConcertService concertService;
    private final Clock clock;

    public WaitingRoomEnterResponse enter(Long userId, WaitingRoomEnterRequest request) {
        concertService.getActiveConcert(request.concertId());

        String token = waitingRoomRedisRepository.getUserToken(request.concertId(), userId)
                .orElseGet(() -> issueWaitingToken(request.concertId(), userId));
        long rank = waitingRoomRedisRepository.rank(request.concertId(), token)
                .map(value -> value + 1)
                .orElse(0L);

        boolean allowed = waitingRoomRedisRepository.isAllowed(request.concertId(), token, userId);
        WaitingTokenStatus status = allowed ? WaitingTokenStatus.ALLOWED : WaitingTokenStatus.WAITING;
        return new WaitingRoomEnterResponse(
                request.concertId(),
                token,
                rank,
                estimateWaitSeconds(rank),
                status
        );
    }

    public WaitingRoomStatusResponse getStatus(Long userId, Long concertId, String waitingToken) {
        concertService.getActiveConcert(concertId);

        if (!StringUtils.hasText(waitingToken)) {
            throw new BusinessException(ErrorCode.WAITING_TOKEN_EXPIRED);
        }

        boolean allowed = waitingRoomRedisRepository.isAllowed(concertId, waitingToken, userId);
        long queueLength = waitingRoomRedisRepository.queueLength(concertId);
        long rank = waitingRoomRedisRepository.rank(concertId, waitingToken)
                .map(value -> value + 1)
                .orElse(allowed ? 0L : -1L);

        if (!allowed && rank < 0) {
            throw new BusinessException(ErrorCode.WAITING_TOKEN_EXPIRED);
        }

        return new WaitingRoomStatusResponse(
                concertId,
                waitingToken,
                rank,
                queueLength,
                allowed,
                allowed ? WaitingTokenStatus.ALLOWED : WaitingTokenStatus.WAITING,
                estimateWaitSeconds(rank),
                allowed ? LocalDateTime.now(clock).plus(ENTRY_TOKEN_TTL) : null
        );
    }

    public WaitingRoomValidateResponse validate(Long userId, WaitingRoomValidateRequest request) {
        concertService.getActiveConcert(request.concertId());

        if (!waitingRoomRedisRepository.isAllowed(request.concertId(), request.waitingToken(), userId)) {
            throw new BusinessException(ErrorCode.WAITING_TOKEN_NOT_ALLOWED);
        }

        String entryToken = UUID.randomUUID().toString();
        waitingRoomRedisRepository.saveEntryToken(request.concertId(), entryToken, userId, ENTRY_TOKEN_TTL);
        return new WaitingRoomValidateResponse(
                request.concertId(),
                entryToken,
                LocalDateTime.now(clock).plus(ENTRY_TOKEN_TTL)
        );
    }

    public void admit(Long concertId, long limit) {
        concertService.getActiveConcert(concertId);
        long admissionLimit = limit <= 0 ? DEFAULT_ADMISSION_LIMIT : limit;
        Set<String> tokens = waitingRoomRedisRepository.topTokens(concertId, admissionLimit);
        if (tokens == null || tokens.isEmpty()) {
            return;
        }

        for (String token : tokens) {
            Long userId = parseUserId(token);
            waitingRoomRedisRepository.removeFromQueue(concertId, token);
            waitingRoomRedisRepository.allowToken(concertId, token, userId, ENTRY_TOKEN_TTL);
        }
    }

    public void validateEntryTokenIfPresent(Long userId, Long concertId, String entryToken) {
        if (!StringUtils.hasText(entryToken)) {
            return;
        }
        if (waitingRoomRedisRepository.isUsedEntryToken(concertId, entryToken)) {
            throw new BusinessException(ErrorCode.WAITING_TOKEN_EXPIRED);
        }
        if (!waitingRoomRedisRepository.consumeEntryToken(concertId, entryToken, userId)) {
            throw new BusinessException(ErrorCode.WAITING_TOKEN_NOT_ALLOWED);
        }
    }

    private String issueWaitingToken(Long concertId, Long userId) {
        String token = "%d:%s".formatted(userId, UUID.randomUUID());
        double score = (double) System.currentTimeMillis();
        waitingRoomRedisRepository.enqueue(concertId, token, score);
        waitingRoomRedisRepository.saveUserToken(concertId, userId, token, USER_TOKEN_TTL);
        return token;
    }

    private Long parseUserId(String token) {
        int delimiterIndex = token.indexOf(':');
        if (delimiterIndex <= 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return Long.parseLong(token.substring(0, delimiterIndex));
    }

    private long estimateWaitSeconds(long rank) {
        if (rank <= 0) {
            return 0;
        }
        return ((rank - 1) / DEFAULT_ADMISSION_LIMIT + 1) * ESTIMATED_SECONDS_PER_100_USERS;
    }
}
