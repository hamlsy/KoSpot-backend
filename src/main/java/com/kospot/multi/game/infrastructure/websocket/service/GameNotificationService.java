package com.kospot.multi.game.infrastructure.websocket.service;

import com.kospot.doc.infrastructure.annotation.WebSocketDoc;
import com.kospot.multi.game.infrastructure.websocket.constants.MultiGameChannelConstants;
import com.kospot.multi.game.application.message.LoadingStatusMessage;
import com.kospot.multi.common.flow.message.RoomGameStartMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    @WebSocketDoc(
        trigger = "게임 시작할 때",
        description = "특정 게임 방에 게임 시작 알림 메시지를 방송합니다.",
        destination = MultiGameChannelConstants.PREFIX_GAME + "{roomId}/start",
        payloadType = RoomGameStartMessage.class
    )
    public void broadcastGameStart(String roomId, RoomGameStartMessage message) {
        try {
            String destination = MultiGameChannelConstants.getStartGameChannel(roomId);
            messagingTemplate.convertAndSend(destination, message);
            log.info("Broadcast game start - RoomId: {}, GameId: {}, RoundId: {}", roomId, message.getGameId(), message.getRoundId());
        } catch (Exception e) {
            log.error("Failed to broadcast game start - RoomId: {}", roomId, e);
        }
    }

    @WebSocketDoc(
        trigger = "게임 창으로 넘어갈 때",
        description = "로딩 상태 알림 메시지를 방송합니다.",
        destination = MultiGameChannelConstants.PREFIX_GAME + "{roomId}/loading/status",
        payloadType = LoadingStatusMessage.class
    )
    public void broadcastLoadingStatus(String roomId, LoadingStatusMessage message) {
        try {
            String destination = MultiGameChannelConstants.getLoadingStatusChannel(roomId);
            messagingTemplate.convertAndSend(destination, message);
            log.info("Broadcast loading status - RoomId: {}, AllArrived: {}", roomId, message.isAllArrived());
        } catch (Exception e) {
            log.error("Failed to broadcast loading status - RoomId: {}", roomId, e);
        }
    }

    @WebSocketDoc(
        trigger = "모든 플레이어가 퇴장하여 게임이 취소될 때",
        description = "게임 취소 알림 메시지를 방송합니다.",
        destination = MultiGameChannelConstants.PREFIX_GAME + "{roomId}/cancelled",
        payloadType = GameCancelledMessage.class
    )
    public void broadcastGameCancelled(String roomId, Long gameId, String reason) {
        try {
            GameCancelledMessage message = GameCancelledMessage.builder()
                    .gameId(gameId)
                    .reason(reason)
                    .timestamp(System.currentTimeMillis())
                    .build();
            String destination = MultiGameChannelConstants.PREFIX_GAME + roomId + "/cancelled";
            messagingTemplate.convertAndSend(destination, message);
            log.info("Broadcast game cancelled - RoomId: {}, GameId: {}, Reason: {}", roomId, gameId, reason);
        } catch (Exception e) {
            log.error("Failed to broadcast game cancelled - RoomId: {}, GameId: {}", roomId, gameId, e);
        }
    }

    @lombok.Builder
    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class GameCancelledMessage {
        private Long gameId;
        private String reason;
        private Long timestamp;
    }
}
