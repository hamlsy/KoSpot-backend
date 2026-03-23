package com.kospot.game.infrastructure.redis.adaptor;

import com.kospot.common.annotation.adaptor.Adaptor;
import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.game.infrastructure.redis.constant.AnonymousPracticeRedisKeyConstants;
import com.kospot.game.infrastructure.redis.dao.AnonymousPracticeTokenRedisRepository;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Adaptor
@RequiredArgsConstructor
public class AnonymousPracticeTokenRedisAdaptor {

    private static final long TOKEN_EXPIRE_HOURS = 2L;

    private final AnonymousPracticeTokenRedisRepository repository;

    public String generateAndStore(Long gameId) {
        String token = UUID.randomUUID().toString();
        repository.save(AnonymousPracticeRedisKeyConstants.getTokenKey(gameId), token, TOKEN_EXPIRE_HOURS);
        return token;
    }

    public void validate(Long gameId, String token) {
        if (token == null) {
            throw new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED);
        }
        String stored = repository.find(AnonymousPracticeRedisKeyConstants.getTokenKey(gameId));
        if (stored == null || !stored.equals(token)) {
            throw new GameHandler(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID);
        }
    }

    public void delete(Long gameId) {
        repository.delete(AnonymousPracticeRedisKeyConstants.getTokenKey(gameId));
    }
}
