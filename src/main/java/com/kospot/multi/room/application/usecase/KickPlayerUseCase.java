package com.kospot.multi.room.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.application.adaptor.GameRoomAdaptor;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.application.service.service.GameRoomService;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.common.annotation.usecase.UseCase;

import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;
import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
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

    public void execute(Long hostId, GameRoomRequest.Kick request, Long gameRoomId) {
        Member host = memberAdaptor.queryById(hostId);
        Long targetPlayerId = request.getTargetPlayerId();
        
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        Member targetPlayer = memberAdaptor.queryById(targetPlayerId);
        
        gameRoomService.validateKickPermission(gameRoom, host, targetPlayerId);
        
        gameRoomService.kickPlayer(host, targetPlayer, gameRoom);
        
        gameRoomRedisService.removePlayerFromRoom(gameRoomId.toString(), targetPlayerId);
        
        GameRoomPlayerInfo targetPlayerInfo = GameRoomPlayerInfo.from(targetPlayer, null,false);
        notificationService.notifyPlayerKicked(gameRoomId.toString(), targetPlayerInfo);
        
        log.info("Player kicked - HostId: {}, TargetId: {}, RoomId: {}", 
                host.getId(), targetPlayerId, gameRoomId);
    }
}
