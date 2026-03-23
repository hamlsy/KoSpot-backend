package com.kospot.game.infrastructure.redis;

import com.kospot.common.exception.object.domain.GameHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.game.infrastructure.redis.constant.AnonymousPracticeRedisKeyConstants;
import com.kospot.game.infrastructure.redis.dao.AnonymousPracticeTokenRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnonymousPracticeTokenRedisAdaptor 단위 테스트")
class AnonymousPracticeTokenRedisAdaptorTest {

    @Mock
    private AnonymousPracticeTokenRedisRepository repository;

    @InjectMocks
    private AnonymousPracticeTokenRedisAdaptor adaptor;

    private static final Long GAME_ID = 1L;

    @Test
    @DisplayName("generateAndStore - UUID 토큰이 저장됨")
    void generateAndStore_savesTokenToRedis() {
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);

        String token = adaptor.generateAndStore(GAME_ID);

        verify(repository).save(keyCaptor.capture(), tokenCaptor.capture(), ttlCaptor.capture());
        assertThat(token).isNotBlank();
        assertThat(tokenCaptor.getValue()).isEqualTo(token);
        assertThat(keyCaptor.getValue()).isEqualTo(AnonymousPracticeRedisKeyConstants.getTokenKey(GAME_ID));
        assertThat(ttlCaptor.getValue()).isEqualTo(2L);
    }

    @Test
    @DisplayName("validate - null 토큰이면 ANONYMOUS_PRACTICE_TOKEN_REQUIRED 예외")
    void validate_nullToken_throwsRequired() {
        assertThatThrownBy(() -> adaptor.validate(GAME_ID, null))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_REQUIRED));
    }

    @Test
    @DisplayName("validate - 저장된 토큰과 일치하면 예외 없음")
    void validate_correctToken_noException() {
        String token = "valid-token";
        when(repository.find(AnonymousPracticeRedisKeyConstants.getTokenKey(GAME_ID))).thenReturn(token);

        adaptor.validate(GAME_ID, token); // no exception
    }

    @Test
    @DisplayName("validate - 저장된 토큰과 불일치하면 ANONYMOUS_PRACTICE_TOKEN_INVALID 예외")
    void validate_wrongToken_throwsInvalid() {
        when(repository.find(anyString())).thenReturn("stored-token");

        assertThatThrownBy(() -> adaptor.validate(GAME_ID, "wrong-token"))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID));
    }

    @Test
    @DisplayName("validate - Redis에 토큰이 없으면 ANONYMOUS_PRACTICE_TOKEN_INVALID 예외")
    void validate_expiredToken_throwsInvalid() {
        when(repository.find(anyString())).thenReturn(null);

        assertThatThrownBy(() -> adaptor.validate(GAME_ID, "any-token"))
                .isInstanceOf(GameHandler.class)
                .satisfies(ex -> assertThat(((GameHandler) ex).getCode())
                        .isEqualTo(ErrorStatus.ANONYMOUS_PRACTICE_TOKEN_INVALID));
    }

    @Test
    @DisplayName("delete - 올바른 키로 Redis에서 삭제됨")
    void delete_callsRepositoryWithCorrectKey() {
        adaptor.delete(GAME_ID);
        verify(repository).delete(AnonymousPracticeRedisKeyConstants.getTokenKey(GAME_ID));
    }
}
