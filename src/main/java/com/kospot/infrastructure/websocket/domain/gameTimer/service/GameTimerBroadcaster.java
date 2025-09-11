package com.kospot.infrastructure.websocket.domain.gameTimer.service;

import com.kospot.infrastructure.websocket.domain.gameTimer.constants.MultiGameChannelConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GameTimerBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    // 1초마다 게임 방 내 플레이어들에게 브로드캐스트
    public void broadcastTimer(String roomId, long remainingTimeMs) {
        messagingTemplate.convertAndSend(
                MultiGameChannelConstants.getTimerChannel(roomId),
                new TimerDto(remainingTimeMs)
        );
    }

    public record TimerDto(long remainingTimeMs) {
    }
}
