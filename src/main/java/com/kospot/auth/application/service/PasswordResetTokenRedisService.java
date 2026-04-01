package com.kospot.auth.application.service;

import com.kospot.auth.infrastructure.redis.PasswordResetTokenRedisRepository;
import com.kospot.common.config.mail.PasswordResetProperties;
import com.kospot.member.domain.exception.MemberErrorStatus;
import com.kospot.member.domain.exception.MemberHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenRedisService {

    private final PasswordResetTokenRedisRepository repository;
    private final PasswordResetProperties properties;

    public String generateAndSave(Long memberId) {
        String token = UUID.randomUUID().toString();
        repository.saveToken(token, memberId, properties.getTokenTtlMinutes());
        return token;
    }

    public Long getAndInvalidate(String token) {
        return repository.getAndDeleteToken(token)
                .orElseThrow(() -> new MemberHandler(MemberErrorStatus.PASSWORD_RESET_TOKEN_INVALID));
    }
}
