package com.kospot.application.multi.room.handler;

import com.kospot.application.multi.room.vo.LeaveDecision;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomJoinEvent;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomEventHandler {

    private final GameRoomNotificationService gameRoomNotificationService;

    @Async
    @EventListener
    public void handleJoin(GameRoomJoinEvent event) {
        String roomId = event.getRoomId().toString();
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                .memberId(event.getMemberId())
                .markerImageUrl(event.getMarkerImageUrl())
                .isHost(event.isHost())
                .nickname(event.getNickname())
                .team(event.getTeam())
                .joinedAt(System.currentTimeMillis())
                .build();

        // STOMP 알림만 전송
        gameRoomNotificationService.notifyPlayerJoined(roomId, playerInfo);
        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

    @EventListener
    public void handleLeave(GameRoomLeaveEvent event) {
        LeaveDecision decision = event.getDecision();
        GameRoom gameRoom = event.getGameRoom();
        String roomId = gameRoom.getId().toString();
        GameRoomPlayerInfo playerInfo = event.getPlayerInfo();

        // STOMP 알림만 전송
        if (playerInfo != null) {
            gameRoomNotificationService.notifyPlayerLeft(roomId, playerInfo);
            log.info("Player left - MemberId: {}, RoomId: {}, Nickname: {}",
                    event.getLeavingMember().getId(), roomId, playerInfo.getNickname());
        }

        // LeaveDecision에 따른 추가 알림
        switch (decision.getAction()) {
            case DELETE_ROOM:
                // 방 삭제 알림은 playerListUpdated로 처리
                break;
            case CHANGE_HOST:
                // 방장 변경 명시적 알림
                GameRoomPlayerInfo newHostInfo = decision.getNewHostInfo();
                gameRoomNotificationService.notifyHostChanged(roomId, newHostInfo);
                break;
            case NORMAL_LEAVE:
                // 별도 처리 없음
                break;
        }

        // 플레이어 리스트 업데이트 (방장 변경 포함)
        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }
}
