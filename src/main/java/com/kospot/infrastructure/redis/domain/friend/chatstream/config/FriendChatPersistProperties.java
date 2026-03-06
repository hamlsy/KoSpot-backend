package com.kospot.infrastructure.redis.domain.friend.chatstream.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "chat.persist")
public class FriendChatPersistProperties {

    private boolean enabled = true;
    private boolean friendOnly = true;
    private String streamKey = "chat:stream";
    private String dlqStreamKey = "chat:stream:dlq";
    private String group = "chat-persist-group";
    private String consumerName = "kospot-friend-chat-worker";
    private int batchSize = 500;
    private int readCount = 500;
    private long readBlockMs = 2000L;
    private long flushIntervalMs = 1000L;
    private int maxRetry = 5;
    private long shutdownTimeoutMs = 10000L;
    private long initRetryDelayMs = 1000L;
    private long errorLogThrottleMs = 30000L;
    private boolean initFailFast = true;
}
