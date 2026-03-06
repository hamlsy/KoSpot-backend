package com.kospot.common.redis.domain.chat.transientstore.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "chat.transient")
public class TransientChatProperties {

    private boolean enabled = true;
    private long ttlDays = 14;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getTtlDays() {
        return ttlDays;
    }

    public void setTtlDays(long ttlDays) {
        this.ttlDays = ttlDays;
    }
}
