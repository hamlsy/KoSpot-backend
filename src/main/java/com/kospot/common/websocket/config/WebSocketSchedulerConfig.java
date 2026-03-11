package com.kospot.common.websocket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class WebSocketSchedulerConfig {

    @Bean(name = "webSocketHeartbeatTaskScheduler")
    public TaskScheduler webSocketHeartbeatTaskScheduler(WebSocketHeartbeatProperties webSocketHeartbeatProperties) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(webSocketHeartbeatProperties.getHeartbeat().getSchedulerPoolSize());
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.initialize();
        return scheduler;
    }
}
