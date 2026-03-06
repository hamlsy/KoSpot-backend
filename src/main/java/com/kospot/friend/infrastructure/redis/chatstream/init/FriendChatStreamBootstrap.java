package com.kospot.friend.infrastructure.redis.chatstream.init;

import com.kospot.friend.infrastructure.redis.chatstream.config.FriendChatPersistProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendChatStreamBootstrap implements ApplicationRunner {

    private final FriendChatPersistProperties properties;
    private final FriendChatStreamInitializer streamInitializer;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            streamInitializer.ensureGroup();
            log.debug("Friend chat stream bootstrap completed. streamKey={}, group={}",
                    properties.getStreamKey(), properties.getGroup());
        } catch (Exception e) {
            if (properties.isInitFailFast()) {
                throw e;
            }
            log.warn("Friend chat stream bootstrap initialization failed. startup will continue with worker self-heal. streamKey={}, group={}, reason={}",
                    properties.getStreamKey(), properties.getGroup(), e.getMessage());
        }
    }
}
