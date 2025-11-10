package com.kospot.infrastructure.websocket.domain.multi.game.service;

import com.kospot.infrastructure.websocket.domain.multi.game.constants.MultiGameChannelConstants;
import com.kospot.application.multi.game.message.LoadingStatusMessage;
import com.kospot.presentation.multi.flow.dto.message.RoomGameStartMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastGameStart(String roomId, RoomGameStartMessage message) {
        try {
            String destination = MultiGameChannelConstants.getStartGameChannel(roomId);
            messagingTemplate.convertAndSend(destination, message);
            log.info("Broadcast game start - RoomId: {}, GameId: {}, RoundId: {}", roomId, message.getGameId(), message.getRoundId());
        } catch (Exception e) {
            log.error("Failed to broadcast game start - RoomId: {}", roomId, e);
        }
    }

    public void broadcastLoadingStatus(String roomId, LoadingStatusMessage message) {
        try {
            String destination = MultiGameChannelConstants.getLoadingStatusChannel(roomId);
            messagingTemplate.convertAndSend(destination, message);
            log.info("Broadcast loading status - RoomId: {}, AllArrived: {}", roomId, message.isAllArrived());
        } catch (Exception e) {
            log.error("Failed to broadcast loading status - RoomId: {}", roomId, e);
        }
    }
}
