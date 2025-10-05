package com.kospot.infrastructure.redis.domain.multi.room.service;

import com.kospot.domain.multi.room.service.GameRoomPlayerService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 게임방 WebSocket 세션 관리자
 * WebSocket 구독/해제 및 세션 관리만 담당 (단일 책임 원칙)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameRoomSessionManager {

    // URL Pattern for Room ID extraction
    private static final Pattern ROOM_ID_PATTERN = Pattern.compile("/room/(\\d+)/");

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomPlayerService gameRoomPlayerService;

    /**
     * WebSocket 구독 처리 및 플레이어 추가
     */
    public void addSubscription(WebSocketMemberPrincipal principal, String roomId, String destination, String sessionId) {
        try {
            // 세션 정보 저장
            gameRoomRedisService.saveSessionInfo(sessionId, roomId, destination, principal.getMemberId());

            log.info("Added WebSocket subscription - MemberId: {}, RoomId: {}, Destination: {}", 
                    principal.getMemberId(), roomId, destination);

        } catch (Exception e) {
            log.error("Failed to add WebSocket subscription - MemberId: {}, RoomId: {}, Error: {}", 
                    principal.getMemberId(), roomId, e.getMessage());
            throw e;
        }
    }

    /**
     * WebSocket 연결 해제 시 플레이어 제거
     */
    public void removePlayerOnDisconnect(String sessionId) {
        try {
            gameRoomPlayerService.removePlayerFromRoom(sessionId);
            log.info("Removed player on WebSocket disconnect - SessionId: {}", sessionId);
            
        } catch (Exception e) {
            log.error("Failed to remove player on disconnect - SessionId: {}, Error: {}", 
                    sessionId, e.getMessage());
        }
    }

    /**
     * destination URL에서 룸 ID 추출
     */
    public String extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }
        
        Matcher matcher = ROOM_ID_PATTERN.matcher(destination);
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        return null;
    }

    /**
     * 플레이어 목록 구독인지 확인
     */
    private boolean isPlayerListSubscription(String destination) {
        return destination.contains("/players") || destination.contains("/playerList");
    }

    /**
     * 게임방 현재 인원 수 조회 (위임)
     */
    public int getCurrentPlayerCount(String roomId) {
        return gameRoomPlayerService.getCurrentPlayerCount(roomId);
    }

    /**
     * 게임방 현재 인원 수 조회 (Long roomId용)
     */
    public int getCurrentPlayerCount(Long roomId) {
        return gameRoomPlayerService.getCurrentPlayerCount(roomId);
    }

    /**
     * 게임방이 비어있는지 확인 (위임)
     */
    public boolean isRoomEmpty(String roomId) {
        return gameRoomPlayerService.isRoomEmpty(roomId);
    }

    /**
     * 게임방 입장 가능 여부 확인 (위임)
     */
    public boolean canJoinRoom(String roomId, int maxPlayers) {
        return gameRoomPlayerService.canJoinRoom(roomId, maxPlayers);
    }

    /**
     * 플레이어 강퇴 (위임)
     */
    public void kickPlayer(Long roomId, Long hostMemberId, Long targetMemberId) {
        gameRoomPlayerService.kickPlayer(roomId, hostMemberId, targetMemberId);
    }

    /**
     * 플레이어 제거 (위임)
     */
    public void removePlayerFromRoom(Long roomId, Long memberId) {
        gameRoomPlayerService.removePlayerFromRoom(roomId, memberId);
    }

}
