package com.kospot.application.multi.room.handler;

import com.kospot.application.multi.room.vo.LeaveDecision;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomJoinEvent;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomEventHandler {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;
    private final GameRoomService gameRoomService;
    private final MemberAdaptor memberAdaptor;

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


        gameRoomRedisService.savePlayerToRoom(roomId, playerInfo);
        gameRoomNotificationService.notifyPlayerJoined(roomId, playerInfo);
        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLeave(GameRoomLeaveEvent event) {
        LeaveDecision decision = event.getDecision();
        GameRoom gameRoom = event.getGameRoom();
        String roomId = gameRoom.getId().toString();

        Member player = event.getLeavingMember();
        Long playerId = player.getId();

        GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(roomId, playerId);
        gameRoomRedisService.cleanupPlayerSession(playerId);

        if (playerInfo != null) {
            gameRoomNotificationService.notifyPlayerLeft(roomId, playerInfo);
            log.info("Player left - MemberId: {}, RoomId: {}, Nickname: {}",
                    playerId, roomId, playerInfo.getNickname());
        }

        switch (decision.getAction()) {
            case DELETE_ROOM -> {
                gameRoomRedisService.deleteRoomData(roomId);
                log.info("Game room deleted - RoomId: {}", roomId);
            }
            case CHANGE_HOST -> {
                changeHost(gameRoom, decision.getNewHostInfo());
            }
            case NORMAL_LEAVE -> {
                // 별도 처리 없음
            }
        }

        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

    // todo redis lock
    @Async("taskExecutor")
    public void changeHost(GameRoom gameRoom, GameRoomPlayerInfo newHostInfo) {
        Long roomId = gameRoom.getId();
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId.toString());

        if (players.isEmpty()) {
            return;
        }

        newHostInfo.setHost(true);
        gameRoomRedisService.savePlayerToRoom(roomId.toString(), newHostInfo);

        log.info("Host changed - RoomId: {}, NewHostId: {}, NewHostName: {}",
                roomId, newHostInfo.getMemberId(), newHostInfo.getNickname());
    }
}
