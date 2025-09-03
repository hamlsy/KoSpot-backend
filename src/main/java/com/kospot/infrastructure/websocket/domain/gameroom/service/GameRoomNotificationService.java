package com.kospot.infrastructure.websocket.domain.gameroom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomNotification;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomNotificationType;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomUpdateInfo;
import com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants;
import com.kospot.infrastructure.websocket.domain.gameroom.constants.GameRoomChannelConstants;
import com.kospot.presentation.multigame.gameroom.dto.event.GameRoomUpdateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.kospot.infrastructure.websocket.domain.gameroom.constants.GameRoomChannelConstants.PREFIX_GAME_ROOM;

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

    /**
     * 플레이어 입장 알림 전송
     */
    public void notifyPlayerJoined(String roomId, GameRoomPlayerInfo playerInfo) {
        try {
            List<GameRoomPlayerInfo> allPlayers = gameRoomRedisService.getRoomPlayers(roomId);
            GameRoomNotification notification = GameRoomNotification.playerJoined(roomId, playerInfo, allPlayers);
            
            String destination = GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId);
            messagingTemplate.convertAndSend(destination, notification);
            
            log.info("Sent player joined notification - RoomId: {}, PlayerId: {}, PlayerName: {}", 
                    roomId, playerInfo.getMemberId(), playerInfo.getNickname());
                    
        } catch (Exception e) {
            log.error("Failed to send player joined notification - RoomId: {}, PlayerId: {}", 
                    roomId, playerInfo.getMemberId(), e);
        }
    }

    /**
     * 플레이어 퇴장 알림 전송
     */
    public void notifyPlayerLeft(String roomId, GameRoomPlayerInfo playerInfo) {
        try {
            List<GameRoomPlayerInfo> allPlayers = gameRoomRedisService.getRoomPlayers(roomId);
            GameRoomNotification notification = GameRoomNotification.playerLeft(roomId, playerInfo, allPlayers);
            
            String destination = GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId);
            messagingTemplate.convertAndSend(destination, notification);
            
            log.info("Sent player left notification - RoomId: {}, PlayerId: {}, PlayerName: {}", 
                    roomId, playerInfo.getMemberId(), playerInfo.getNickname());
                    
        } catch (Exception e) {
            log.error("Failed to send player left notification - RoomId: {}, PlayerId: {}", 
                    roomId, playerInfo.getMemberId(), e);
        }
    }

    /**
     * 플레이어 강퇴 알림 전송
     */
    public void notifyPlayerKicked(String roomId, Member kickedMember) {
        try {
            GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(kickedMember);
            List<GameRoomPlayerInfo> allPlayers = gameRoomRedisService.getRoomPlayers(roomId);
            GameRoomNotification notification = GameRoomNotification.playerKicked(roomId, playerInfo, allPlayers);
            
            String destination = GameRoomChannelConstants.getGameRoomPlayerListChannel(roomId);
            messagingTemplate.convertAndSend(destination, notification);
            
            log.info("Sent player kicked notification - RoomId: {}, PlayerId: {}, PlayerName: {}", 
                    roomId, kickedMember.getId(), kickedMember.getNickname());
                    
        } catch (Exception e) {
            log.error("Failed to send player kicked notification - RoomId: {}, PlayerId: {}", 
                    roomId, kickedMember.getId(), e);
        }
    }


    /**
     * 플레이어 입장 이벤트 (인원 수 포함)
     */
    public void notifyPlayerJoinedWithCount(String roomId, GameRoomPlayerInfo playerInfo, int previousCount, int currentCount) {
        // 플레이어 입장 알림
        notifyPlayerJoined(roomId, playerInfo);
    }

    /**
     * 플레이어 퇴장 이벤트 (인원 수 포함)
     */
    public void notifyPlayerLeftWithCount(String roomId, GameRoomPlayerInfo playerInfo, int previousCount, int currentCount) {
        // 플레이어 퇴장 알림
        notifyPlayerLeft(roomId, playerInfo);

    }

    /**
     * 플레이어 강퇴 이벤트 (인원 수 포함)
     */
    public void notifyPlayerKickedWithCount(String roomId, Member kickedMember, int previousCount, int currentCount) {
        // 플레이어 강퇴 알림
        notifyPlayerKicked(roomId, kickedMember);
    }

    /**
     * 방 설정 변경 알림
     */
    public void notifyRoomSettingsChanged(String roomId, GameRoomUpdateInfo updateInfo) {
        try {

            GameRoomUpdateEvent event = GameRoomUpdateEvent.of(updateInfo);
            String destination = GameRoomChannelConstants.getGameRoomSettingsChannel(roomId);
            String message = objectMapper.writeValueAsString(event);
            
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