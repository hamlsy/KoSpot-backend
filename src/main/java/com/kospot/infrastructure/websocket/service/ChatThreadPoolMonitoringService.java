package com.kospot.infrastructure.websocket.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component
public class ChatThreadPoolMonitoringService {

    private final ThreadPoolTaskExecutor chatRoomExecutor;
    private final ThreadPoolTaskExecutor chatMessageExecutor;
    private final ThreadPoolTaskExecutor chatBroadcastExecutor;

    public ChatThreadPoolMonitoringService(
            @Qualifier("chatRoomExecutor") ThreadPoolTaskExecutor chatRoomExecutor,
            @Qualifier("chatMessageExecutor") ThreadPoolTaskExecutor chatMessageExecutor,
            @Qualifier("chatBroadcastExecutor") ThreadPoolTaskExecutor chatBroadcastExecutor) {
        this.chatRoomExecutor = chatRoomExecutor;
        this.chatMessageExecutor = chatMessageExecutor;
        this.chatBroadcastExecutor = chatBroadcastExecutor;
    }

    @Scheduled(fixedDelay = 30000) // 30초마다 실행
    public void monitorChatThreadPools() {
        logThreadPoolStatus("ChatRoom", chatRoomExecutor);
        logThreadPoolStatus("ChatMessage", chatMessageExecutor);
        logThreadPoolStatus("ChatBroadcast", chatBroadcastExecutor);

        // CPU 크레딧 절약을 위한 경고 로직
        checkResourceUsage();
    }
    private void logThreadPoolStatus(String poolName, ThreadPoolTaskExecutor executor) {
        ThreadPoolExecutor threadPoolExecutor = executor.getThreadPoolExecutor();

        log.info("{} ThreadPool Status - Active: {}, Queue: {}, Pool Size: {}, Core Pool: {}, Max Pool: {}",
                poolName,
                threadPoolExecutor.getActiveCount(),
                threadPoolExecutor.getQueue().size(),
                threadPoolExecutor.getPoolSize(),
                threadPoolExecutor.getCorePoolSize(),
                threadPoolExecutor.getMaximumPoolSize());
    }

    private void checkResourceUsage() {
        // 메모리 사용량 체크 (EC2 t2.micro 1GB 제한)
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        double memoryUsagePercentage = (double) usedMemory / maxMemory * 100;

        if (memoryUsagePercentage > 80) {
            log.warn("High memory usage detected: {}% - Consider reducing thread pool sizes",
                    String.format("%.2f", memoryUsagePercentage));
        }

        log.debug("Memory Status - Used: {}MB, Total: {}MB, Max: {}MB, Usage: {}%",
                usedMemory / (1024 * 1024),
                totalMemory / (1024 * 1024),
                maxMemory / (1024 * 1024),
                String.format("%.2f", memoryUsagePercentage));
    }


}
