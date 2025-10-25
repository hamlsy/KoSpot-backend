package com.kospot.infrastructure.websocket.domain.multi.round.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.application.multi.round.message.GameFinishedMessage;
import com.kospot.domain.multi.game.entity.MultiGame;
import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoundNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 라운드 결과를 모든 플레이어에게 브로드캐스트
     */
    public void broadcastRoundResults(String roomId, Object notification) {
        sendNotification(roomId, notification, MultiGameChannelConstants.getRoundResultChannel(roomId));
    }

    /**
     * 라운드 시작을 모든 플레이어에게 브로드캐스트
     */
    public void broadcastRoundStart(String roomId, Object notification) {
        sendNotification(roomId, notification, MultiGameChannelConstants.getRoundStartChannel(roomId));
    }

//    public void broadcastRoundEnd(String roomId, Long gameId) {
//        String destination = MultiGameChannelConstants.getRoundResultChannel()
//        sendNotification(roomId, notification, MultiGameChannelConstants.getRoundEndChannel(roomId));
//    }

    private void sendNotification(String roomId, Object notification, String destination) {
        try {
            messagingTemplate.convertAndSend(destination, notification);
            log.info("Round notification sent - RoomId: {}, Destination: {}", roomId, destination);
        } catch (Exception e) {
            log.error("Failed to send round notification - RoomId: {}, Destination: {}", roomId, destination, e);
        }
    }

    public void notifyGameFinished(String roomId, Long gameId) {
        String destination = MultiGameChannelConstants.getGameFinishChannel(roomId);
        GameFinishedMessage message = GameFinishedMessage.builder()
                .gameId(gameId)
                .message("게임이 종료되었습니다.")
                .timestamp(System.currentTimeMillis())
                .build();
        messagingTemplate.convertAndSend(destination, message);
    }
}
