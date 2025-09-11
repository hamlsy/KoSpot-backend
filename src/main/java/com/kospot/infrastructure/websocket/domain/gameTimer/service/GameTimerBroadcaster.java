package com.kospot.infrastructure.websocket.domain.gameTimer.service;

import com.kospot.domain.multigame.gameTimer.service.GameTimerService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameTimerBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private final GameTimerService gameTimerService;

    private static final String TEMP_CHANNEL = "/topic/game/timer";

    // 1초마다 게임 방 내 플레이어들에게 브로드캐스트
    @Scheduled(fixedRate = 1000)
    public void broadcastTimer() {
        if (!gameTimerService.isRunning()) return;

        messagingTemplate.convertAndSend(TEMP_CHANNEL,
                new TimerDto(gameTimerService.getRemainingTimeMs())
        );
    }

    public record TimerDto(long remainingTimeMs) {
    }
}
