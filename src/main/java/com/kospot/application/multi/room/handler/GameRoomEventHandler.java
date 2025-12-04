package com.kospot.application.multi.room.handler;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomJoinEvent;
import com.kospot.domain.multi.room.event.GameRoomLeaveEvent;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.redis.domain.multi.room.adaptor.GameRoomRedisAdaptor;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameRoomEventHandler {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisAdaptor gameRoomRedisAdaptor;
    private final MemberAdaptor memberAdaptor;

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


        gameRoomRedisService.addPlayerToRoom(roomId, playerInfo);

        gameRoomNotificationService.notifyPlayerJoined(roomId, playerInfo);

        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

    @EventListener
    public void handleLeave(GameRoomLeaveEvent event) {
        GameRoom gameRoom = event.getGameRoom();
        String roomId = gameRoom.getId().toString();
        Member player = event.getLeavingMember();
        Long playerId = player.getId();

        GameRoomPlayerInfo playerInfo = gameRoomRedisService.removePlayerFromRoom(roomId, playerId);

        if (playerInfo != null) {
            gameRoomService.updateMemberLeaveStatus(player);
            gameRoomNotificationService.notifyPlayerLeft(roomId, playerInfo);
            log.info("Player left - MemberId: {}, RoomId: {}, Nickname: {}",
                    playerId, roomId, playerInfo.getNickname());
        }

        gameRoomRedisService.cleanupPlayerSession(playerId);

        // 추후 삭제, 나가는 것만 처리하기
        if (gameRoom.isHost(player) || gameRoomRedisService.isRoomEmpty(roomId)) {
            gameRoomService.deleteRoom(gameRoom);
        } else {
//            changeHostIfNeeded(gameRoom);
        }

        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

    @Async("taskExecutor")
    public void changeHost(GameRoom gameRoom) {
        Long roomId = gameRoom.getId();
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(roomId.toString());

        if (players.isEmpty()) {
            return;
        }

        Optional<GameRoomPlayerInfo> newHostInfo = gameRoomRedisAdaptor.pickNextHostByJoinedAt(
                gameRoom.getId().toString(), gameRoom.getHost().getId()).orElse(
                //
                //이때 방 제거?
        );
        Member newHost = memberAdaptor.queryById(newHostInfo.getMemberId());
        gameRoomService.changeHostToMember(gameRoom, newHost);

        newHostInfo.setHost(true);

        gameRoomRedisService.addPlayerToRoom(roomId.toString(), newHostInfo);

        log.info("Host changed - RoomId: {}, NewHostId: {}, NewHostName: {}",
                roomId, newHostInfo.getMemberId(), newHostInfo.getNickname());
    }
}
