package com.kospot.auth.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class PasswordResetTokenRedisRepository {

    private static final String TOKEN_KEY_PREFIX = "password-reset:";
    private static final String RATE_KEY_PREFIX = "password-reset-rate:";

    private final StringRedisTemplate stringRedisTemplate;

    public void saveToken(String token, Long memberId, long ttlMinutes) {
        stringRedisTemplate.opsForValue()
                .set(TOKEN_KEY_PREFIX + token, String.valueOf(memberId), ttlMinutes, TimeUnit.MINUTES);
    }

    // GETDEL: 조회+삭제 원자적 처리 (Spring Data Redis 3.x / Redis 6.2+)
    public Optional<Long> getAndDeleteToken(String token) {
        String value = stringRedisTemplate.opsForValue().getAndDelete(TOKEN_KEY_PREFIX + token);
        if (value == null) return Optional.empty();
        return Optional.of(Long.parseLong(value));
    }

    // Rate Limit: 최초 요청 시에만 TTL 설정 (count == 1)
    public long incrementRateLimit(String email, long ttlHours) {
        String key = RATE_KEY_PREFIX + email;
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, ttlHours, TimeUnit.HOURS);
        }
        return count == null ? 0L : count;
    }
}
