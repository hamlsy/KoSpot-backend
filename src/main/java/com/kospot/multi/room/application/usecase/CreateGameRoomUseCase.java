package com.kospot.multi.room.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.application.service.service.GameRoomService;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.member.infrastructure.redis.service.MemberProfileRedisService;
import com.kospot.common.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.common.websocket.domain.multi.lobby.service.LobbyRoomNotificationService;
import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
import com.kospot.multi.room.presentation.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CreateGameRoomUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomService gameRoomService;

    private final MemberProfileRedisService memberProfileRedisService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    private final LobbyRoomNotificationService lobbyRoomNotificationService;
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;

    public GameRoomResponse execute(Long hostId, GameRoomRequest.Create request) {
        Member host = memberAdaptor.queryById(hostId);
        autoLeaveIfInRoom(host);
        GameRoom gameRoom = gameRoomService.createGameRoom(host, request);

        // member profile view 설정
        MemberProfileRedisAdaptor.MemberProfileView profileView = memberProfileRedisAdaptor.findProfile(host.getId());

        // redis 설정
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(host, profileView.markerImageUrl(), true);
        gameRoomRedisService.savePlayerToRoom(gameRoom.getId().toString(), playerInfo);


        // notify
        lobbyRoomNotificationService.notifyRoomCreated(gameRoom);

        return GameRoomResponse.from(gameRoom);
    }

    private void autoLeaveIfInRoom(Member host) {
        Long currentRoomId = host.getGameRoomId();
        if (currentRoomId != null) {
            log.info("Auto-leaving room {} before creating new room - MemberId: {}",
                    currentRoomId, host.getId());
            leaveGameRoomUseCase.execute(host.getId(), currentRoomId);
        }
    }

}
