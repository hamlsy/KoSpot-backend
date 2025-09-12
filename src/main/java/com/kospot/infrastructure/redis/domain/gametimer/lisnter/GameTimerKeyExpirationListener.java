package com.kospot.infrastructure.redis.domain.gametimer.lisnter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameTimerKeyExpirationListener implements MessageListener {

    //private final SimpleGameTimerService gameTimerService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());
        // 비동기 실행 → Redis 이벤트 처리 지연 방지
        CompletableFuture.runAsync(() -> {
            try {
//                gameTimerService.handleTimerExpiration(expiredKey);
            } catch (Exception e) {
                log.error("Error handling timer expiration for key: {}", expiredKey, e);
            }
        });
    }
}
