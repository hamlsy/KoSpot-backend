package com.kospot.application.multi.room.http.usecase;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.infrastructure.websocket.domain.multi.room.service.GameRoomNotificationService;
import com.kospot.presentation.multi.gameroom.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class KickPlayerUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final MemberAdaptor memberAdaptor;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService notificationService;

    public void execute(Member host, GameRoomRequest.Kick request, Long gameRoomId) {
        Long targetPlayerId = request.getTargetPlayerId();
        
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        Member targetPlayer = memberAdaptor.queryById(targetPlayerId);
        
        gameRoomService.validateKickPermission(gameRoom, host, targetPlayerId);
        
        gameRoomService.kickPlayer(host, targetPlayer, gameRoom);
        
        gameRoomRedisService.removePlayerFromRoom(gameRoomId.toString(), targetPlayerId);
        
        GameRoomPlayerInfo targetPlayerInfo = GameRoomPlayerInfo.from(targetPlayer, false);
        notificationService.notifyPlayerKicked(gameRoomId.toString(), targetPlayerInfo);
        
        log.info("Player kicked - HostId: {}, TargetId: {}, RoomId: {}", 
                host.getId(), targetPlayerId, gameRoomId);
    }
}
