package com.kospot.multi.room.application.handler;

import com.kospot.multi.room.application.vo.LeaveDecision;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.domain.event.GameRoomJoinEvent;
import com.kospot.multi.room.domain.event.GameRoomLeaveEvent;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomEventHandler {

    private final GameRoomNotificationService gameRoomNotificationService;
    private final GameRoomRedisService gameRoomRedisService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleJoin(GameRoomJoinEvent event) {
        String roomId = event.getRoomId().toString();
        GameRoomPlayerInfo playerInfo = gameRoomRedisService.getRoomPlayer(roomId, event.getMemberId())
                .orElse(null);

        if (playerInfo == null) {
            log.warn("Skip player joined delta notification because player info not found in Redis - RoomId: {}, MemberId: {}",
                    roomId,
                    event.getMemberId());
            gameRoomNotificationService.notifyPlayerListUpdated(roomId);
            return;
        }

        // STOMP 알림만 전송 (Redis 저장값 기준)
        gameRoomNotificationService.notifyPlayerJoined(roomId, playerInfo);
        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
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
