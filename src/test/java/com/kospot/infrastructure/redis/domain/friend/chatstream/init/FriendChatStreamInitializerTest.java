package com.kospot.infrastructure.redis.domain.friend.chatstream.init;

import com.kospot.friend.infrastructure.redis.chatstream.config.FriendChatPersistProperties;
import com.kospot.friend.infrastructure.redis.chatstream.init.FriendChatStreamInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendChatStreamInitializerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private FriendChatStreamInitializer initializer;

    @BeforeEach
    void setUp() {
        FriendChatPersistProperties properties = new FriendChatPersistProperties();
        properties.setStreamKey("chat:stream");
        properties.setGroup("chat-persist-group");
        initializer = new FriendChatStreamInitializer(stringRedisTemplate, properties);
    }

    @Test
    void ensureGroup_ignoresBusyGroupError() {
        when(stringRedisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new DataAccessResourceFailureException("BUSYGROUP Consumer Group name already exists"));

        assertDoesNotThrow(() -> initializer.ensureGroup());
    }

    @Test
    void ensureGroup_ignoresBusyGroupErrorInNestedCause() {
        RuntimeException busyGroupCause = new RuntimeException("BUSYGROUP Consumer Group name already exists");
        when(stringRedisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new DataAccessResourceFailureException("Error in execution", busyGroupCause));

        assertDoesNotThrow(() -> initializer.ensureGroup());
    }

    @Test
    void ensureGroup_rethrowsUnexpectedDataAccessError() {
        when(stringRedisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new DataAccessResourceFailureException("ERR generic failure"));

        assertThrows(DataAccessException.class, () -> initializer.ensureGroup());
    }
}
