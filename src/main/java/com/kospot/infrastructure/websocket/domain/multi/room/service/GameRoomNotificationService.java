package com.kospot.infrastructure.websocket.domain.multi.room.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.multi.room.vo.GameRoomNotification;
import com.kospot.domain.multi.room.vo.GameRoomNotificationType;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.domain.multi.room.vo.GameRoomUpdateInfo;
import com.kospot.infrastructure.doc.annotation.WebSocketDoc;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.websocket.domain.multi.room.constants.*;
import com.kospot.presentation.multi.room.dto.message.GameRoomUpdateMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import static com.kospot.infrastructure.websocket.domain.multi.room.constants.GameRoomChannelConstants.PREFIX_GAME_ROOM;

/**
 * 게임방 실시간 알림 전송 서비스
 * WebSocket을 통한 실시간 알림 메시지 전송 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomNotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final GameRoomRedisService gameRoomRedisService;

    private void sendNotification(String roomId, Object notification, String channel) {
        try {
            messagingTemplate.convertAndSend(channel, notification);
        } catch (Exception e) {
            log.error("Failed to send notification - RoomId: {}, Channel: {}", roomId, channel, e);
        }
    }


    /** ---------------- 개별 이벤트 ---------------- **/

    public void notifyPlayerJoined(String roomId, GameRoomPlayerInfo playerInfo) {
        GameRoomNotification notification = GameRoomNotification.playerJoined(roomId, playerInfo);
        sendNotification(roomId, notification, GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId));
        log.info("Player joined - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());
    }

    public void notifyPlayerLeft(String roomId, GameRoomPlayerInfo playerInfo) {
        GameRoomNotification notification = GameRoomNotification.playerLeft(roomId, playerInfo);
        sendNotification(roomId, notification, GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId));
        log.info("Player left - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());
    }

    public void notifyPlayerKicked(String roomId, GameRoomPlayerInfo playerInfo) {
        GameRoomNotification notification = GameRoomNotification.playerKicked(roomId, playerInfo);
        sendNotification(roomId, notification, GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId));
        log.info("Player kicked - RoomId: {}, PlayerId: {}", roomId, playerInfo.getMemberId());
    }

    @WebSocketDoc(
        description = "게임방 플레이어 리스트 갱신 알림",
        destination = PREFIX_GAME_ROOM + "{roomId}/playerList",
        payloadType = GameRoomNotification.class,
        trigger = "플레이어 입장/퇴장 시"
    )
    public void notifyPlayerListUpdated(String roomId) {
        try {
            List<GameRoomPlayerInfo> allPlayers = gameRoomRedisService.getRoomPlayers(roomId);
            GameRoomNotification notification = GameRoomNotification.playerListUpdated(roomId, allPlayers);
            sendNotification(roomId, notification, GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId));
            log.info("Player list updated - RoomId: {}, PlayerCount: {}", roomId, allPlayers.size());
        } catch (Exception e) {
            log.error("Failed to send player list updated - RoomId: {}", roomId, e);
        }
    }
    /**
     * 방 설정 변경 알림
     */
    @WebSocketDoc(
        description = "게임방 설정 변경 알림",
        destination = PREFIX_GAME_ROOM + "{roomId}/settings",
        payloadType = GameRoomUpdateMessage.class,
        trigger = "게임방 설정이 변경될 때마다"
    )
    public void notifyRoomSettingsChanged(String roomId, GameRoomUpdateInfo updateInfo) {
        try {

            GameRoomUpdateMessage updateMessage = GameRoomUpdateMessage.of(updateInfo);
            String destination = GameRoomChannelConstants.getGameRoomSettingsChannel(roomId);
            String message = objectMapper.writeValueAsString(updateMessage);

            messagingTemplate.convertAndSend(destination, message);

            log.info("Sent room settings change notification - RoomId: {}", roomId);

        } catch (Exception e) {
            log.error("Failed to send room settings change notification - RoomId: {}",
                    roomId, e);
        }
    }

    /**
     * 게임 시작 알림
     */
    public void notifyGameStarted(String roomId) {
        try {
            String destination = GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId);
            
            GameRoomNotification notification = GameRoomNotification.builder()
                    .type(GameRoomNotificationType.GAME_STARTED.name())
                    .roomId(roomId)
                    .timestamp(System.currentTimeMillis())
                    .build();
            
            messagingTemplate.convertAndSend(destination, notification);
            
            log.info("Sent game started notification - RoomId: {}", roomId);
            
        } catch (Exception e) {
            log.error("Failed to send game started notification - RoomId: {}", roomId, e);
        }
    }

    /**
     * 커스텀 메시지 전송
     */
    public void sendCustomMessage(String roomId, String channel, Object message) {
        try {
            String destination = PREFIX_GAME_ROOM + roomId + "/" + channel;
            messagingTemplate.convertAndSend(destination, message);
            
            log.debug("Sent custom message - RoomId: {}, Channel: {}", roomId, channel);
            
        } catch (Exception e) {
            log.error("Failed to send custom message - RoomId: {}, Channel: {}", roomId, channel, e);
        }
    }

    /**
     * 에러 메시지 전송
     */
    public void sendErrorMessage(String roomId, String errorCode, String errorMessage) {
        try {
            String destination = GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId);
            
            String message = String.format("{\"type\":\"ERROR\",\"errorCode\":\"%s\",\"message\":\"%s\",\"timestamp\":%d}", 
                    errorCode, errorMessage, System.currentTimeMillis());
            
            messagingTemplate.convertAndSend(destination, message);
            
            log.warn("Sent error message - RoomId: {}, ErrorCode: {}, Message: {}", roomId, errorCode, errorMessage);
            
        } catch (Exception e) {
            log.error("Failed to send error message - RoomId: {}, ErrorCode: {}", roomId, errorCode, e);
        }
    }
} 