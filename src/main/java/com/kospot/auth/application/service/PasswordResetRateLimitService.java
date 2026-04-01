package com.kospot.auth.application.service;

import com.kospot.auth.infrastructure.redis.PasswordResetTokenRedisRepository;
import com.kospot.common.config.mail.PasswordResetProperties;
import com.kospot.common.exception.object.domain.EmailHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetRateLimitService {

    private final PasswordResetTokenRedisRepository repository;
    private final PasswordResetProperties properties;

    public void checkAndIncrement(String email) {
        long count = repository.incrementRateLimit(email, properties.getRateLimitTtlHours());
        if (count > properties.getRateLimitMax()) {
            throw new EmailHandler(ErrorStatus.EMAIL_RATE_LIMIT_EXCEEDED);
        }
    }
}
