package com.kospot.infrastructure.redis.domain.friend.chatstream.worker;

import com.kospot.infrastructure.redis.domain.friend.chatstream.config.FriendChatPersistProperties;
import com.kospot.infrastructure.redis.domain.friend.chatstream.dlq.FriendChatDlqPublisher;
import com.kospot.infrastructure.redis.domain.friend.chatstream.init.FriendChatStreamInitializer;
import com.kospot.infrastructure.redis.domain.friend.chatstream.persistence.FriendChatBatchInsertRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FriendChatPersistWorkerTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @Mock
    private FriendChatBatchInsertRepository batchInsertRepository;

    @Mock
    private FriendChatDlqPublisher dlqPublisher;

    @Mock
    private FriendChatStreamInitializer streamInitializer;

    private FriendChatPersistWorker worker;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        FriendChatPersistProperties properties = new FriendChatPersistProperties();
        properties.setReadBlockMs(10L);
        properties.setReadCount(10);
        properties.setBatchSize(10);
        properties.setFlushIntervalMs(10L);
        properties.setInitRetryDelayMs(10L);

        when(stringRedisTemplate.opsForStream()).thenReturn((StreamOperations) streamOperations);

        worker = new FriendChatPersistWorker(
                stringRedisTemplate,
                properties,
                batchInsertRepository,
                dlqPublisher,
                streamInitializer
        );
    }

    @AfterEach
    void tearDown() {
        worker.stop();
    }

    @Test
    void start_initializesStreamGroupBeforeLoop() throws Exception {
        worker.start();

        Thread.sleep(30L);

        verify(streamInitializer, atLeastOnce()).ensureGroup();
    }
}
