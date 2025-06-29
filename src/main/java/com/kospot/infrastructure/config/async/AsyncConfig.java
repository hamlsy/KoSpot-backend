package com.kospot.infrastructure.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    //todo custom
    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    // ec2 t3.micro 기준
    @Bean("chatRoomExecutor")
    public Executor chatRoomExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);           // 단일 vCPU 기반
        executor.setMaxPoolSize(2);            // 최대 2개 (CPU 크레딧 고려)
        executor.setQueueCapacity(10);         // 작은 큐 크기
        executor.setKeepAliveSeconds(30);      // 빠른 스레드 회수
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("ChatRoom-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("chatMessageExecutor")
    public Executor chatMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);           // 단일 vCPU
        executor.setMaxPoolSize(3);            // 메시지 처리량 고려
        executor.setQueueCapacity(20);         // 글로벌 로비 메시지 대기열
        executor.setKeepAliveSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("ChatMessage-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Bean("chatBroadcastExecutor")
    public Executor chatBroadcastExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);           // 단일 vCPU
        executor.setMaxPoolSize(2);            // 브로드캐스트 전용
        executor.setQueueCapacity(15);
        executor.setKeepAliveSeconds(30);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix("ChatBroadcast-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

