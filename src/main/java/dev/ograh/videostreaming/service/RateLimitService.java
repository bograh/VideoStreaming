package dev.ograh.videostreaming.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    public boolean isAllowed(String key, int limit, long windowMillis) {
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;

        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        zSetOps.removeRangeByScore(key, 0, windowStart);

        Long currentCount = zSetOps.zCard(key);

        if (currentCount != null && currentCount >= limit) {
            return false;
        }

        zSetOps.add(key, UUID.randomUUID().toString(), now);

        redisTemplate.expire(key, Duration.ofMillis(windowMillis));

        return true;
    }

}