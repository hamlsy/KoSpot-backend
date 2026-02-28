package com.kospot.infrastructure.redis.domain.friend.chatstream.init;

import com.kospot.infrastructure.redis.domain.friend.chatstream.config.FriendChatPersistProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendChatStreamBootstrap implements ApplicationRunner {

    private static final String BUSY_GROUP_TOKEN = "BUSYGROUP";

    private final StringRedisTemplate stringRedisTemplate;
    private final FriendChatPersistProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        byte[] streamKey = properties.getStreamKey().getBytes(StandardCharsets.UTF_8);
        byte[] group = properties.getGroup().getBytes(StandardCharsets.UTF_8);

        try {
            stringRedisTemplate.execute((RedisCallback<Object>) connection -> connection.execute(
                    "XGROUP",
                    "CREATE".getBytes(StandardCharsets.UTF_8),
                    streamKey,
                    group,
                    "$".getBytes(StandardCharsets.UTF_8),
                    "MKSTREAM".getBytes(StandardCharsets.UTF_8)
            ));
            log.info("Created friend chat stream consumer group. streamKey={}, group={}", properties.getStreamKey(), properties.getGroup());
        } catch (DataAccessException e) {
            String message = e.getMessage();
            if (message != null && message.contains(BUSY_GROUP_TOKEN)) {
                log.info("Friend chat stream consumer group already exists. streamKey={}, group={}", properties.getStreamKey(), properties.getGroup());
                return;
            }
            throw e;
        }
    }
}
