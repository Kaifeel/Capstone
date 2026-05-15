package com.example.ticketing.waitingroom.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class WaitingRoomRedisRepository {

    private final StringRedisTemplate redisTemplate;

    public void enqueue(Long concertId, String token, double score) {
        redisTemplate.opsForZSet().add(queueKey(concertId), token, score);
    }

    public Optional<Long> rank(Long concertId, String token) {
        Long rank = redisTemplate.opsForZSet().rank(queueKey(concertId), token);
        return Optional.ofNullable(rank);
    }

    public long queueLength(Long concertId) {
        Long size = redisTemplate.opsForZSet().size(queueKey(concertId));
        return size == null ? 0 : size;
    }

    public Set<String> topTokens(Long concertId, long count) {
        return redisTemplate.opsForZSet().range(queueKey(concertId), 0, count - 1);
    }

    public void removeFromQueue(Long concertId, String token) {
        redisTemplate.opsForZSet().remove(queueKey(concertId), token);
    }

    public void saveUserToken(Long concertId, Long userId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(userTokenKey(concertId, userId), token, ttl);
    }

    public Optional<String> getUserToken(Long concertId, Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(userTokenKey(concertId, userId)));
    }

    public void allowToken(Long concertId, String waitingToken, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(allowedKey(concertId, waitingToken), userId.toString(), ttl);
    }

    public boolean isAllowed(Long concertId, String waitingToken, Long userId) {
        String savedUserId = redisTemplate.opsForValue().get(allowedKey(concertId, waitingToken));
        return userId.toString().equals(savedUserId);
    }

    public void saveEntryToken(Long concertId, String entryToken, Long userId, Duration ttl) {
        redisTemplate.opsForValue().set(entryKey(concertId, entryToken), userId.toString(), ttl);
    }

    public boolean consumeEntryToken(Long concertId, String entryToken, Long userId) {
        String key = entryKey(concertId, entryToken);
        String savedUserId = redisTemplate.opsForValue().get(key);
        if (!userId.toString().equals(savedUserId)) {
            return false;
        }
        redisTemplate.delete(key);
        redisTemplate.opsForValue().set(usedEntryKey(concertId, entryToken), userId.toString(), Duration.ofMinutes(10));
        return true;
    }

    public boolean isUsedEntryToken(Long concertId, String entryToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(usedEntryKey(concertId, entryToken)));
    }

    private String queueKey(Long concertId) {
        return "waiting:%d:queue".formatted(concertId);
    }

    private String userTokenKey(Long concertId, Long userId) {
        return "waiting:%d:user:%d".formatted(concertId, userId);
    }

    private String allowedKey(Long concertId, String waitingToken) {
        return "waiting:%d:allowed:%s".formatted(concertId, waitingToken);
    }

    private String entryKey(Long concertId, String entryToken) {
        return "waiting:%d:entry:%s".formatted(concertId, entryToken);
    }

    private String usedEntryKey(Long concertId, String entryToken) {
        return "waiting:%d:used:%s".formatted(concertId, entryToken);
    }
}
