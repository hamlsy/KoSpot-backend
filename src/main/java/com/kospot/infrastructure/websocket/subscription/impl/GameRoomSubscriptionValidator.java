package com.kospot.infrastructure.websocket.subscription.impl;

import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.websocket.subscription.SubscriptionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 게임방 구독 검증자
 * - 게임방 참여 여부 확인
 * - 강퇴 여부 확인
 * - 게임방별 세부 권한 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomSubscriptionValidator implements SubscriptionValidator {
    
    private final GameRoomRedisService gameRoomRedisService;
    
    @Override
    public boolean canSubscribe(WebSocketMemberPrincipal principal, String destination) {
        if (principal == null || principal.getMemberId() == null) {
            log.warn("Game room access denied - No authentication");
            return false;
        }
        
        String roomId = extractRoomIdFromDestination(destination);
        if (roomId == null) {
            log.warn("Game room access denied - Invalid room ID in destination: {}", destination);
            return false;
        }
        
        Long memberId = principal.getMemberId();

        // 게임방 참여 여부 확인
        if (!isPlayerInRoom(roomId, memberId)) {
            log.warn("Game room access denied - Player not in room: MemberId={}, RoomId={}", memberId, roomId);
            return false;
        }
        
        log.debug("Game room access granted - MemberId: {}, RoomId: {}, Destination: {}", 
                memberId, roomId, destination);
        
        return true;
    }
    
    @Override
    public boolean supports(String destination) {
        if (destination == null) return false;
        
        return destination.startsWith("/topic/room/");
    }
    
    @Override
    public int getPriority() {
        return 200; // 게임방은 더 엄격한 검증이 필요하므로 높은 우선순위
    }
    
    /**
     * destination에서 룸 ID 추출
     */
    private String extractRoomIdFromDestination(String destination) {
        return WebSocketChannelConstants.extractRoomIdFromDestination(destination);
    }
    
    /**
     * 플레이어가 게임방에 참여 중인지 확인
     */
    private boolean isPlayerInRoom(String roomId, Long memberId) {
        try {
            List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId);
            
            boolean isInRoom = players.stream()
                    .anyMatch(player -> memberId.equals(player.getMemberId()));
            
            log.debug("Player room membership check - MemberId: {}, RoomId: {}, IsInRoom: {}, PlayerCount: {}", 
                    memberId, roomId, isInRoom, players.size());
            
            return isInRoom;
            
        } catch (Exception e) {
            log.error("Failed to check player room membership - MemberId: {}, RoomId: {}", 
                    memberId, roomId, e);
            return false;
        }
    }
}
