package com.kospot.multi.room.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.application.service.AutoLeaveRecoveryService;
import com.kospot.multi.room.application.vo.ReconcileResult;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.application.service.service.GameRoomService;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.member.infrastructure.redis.service.MemberProfileRedisService;

import com.kospot.multi.lobby.infrastructure.websocket.service.LobbyRoomNotificationService;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
import com.kospot.multi.room.presentation.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.kospot.common.exception.object.domain.GameRoomHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
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
    private final AutoLeaveRecoveryService autoLeaveRecoveryService;

    private final LobbyRoomNotificationService lobbyRoomNotificationService;

    public GameRoomResponse execute(Long hostId, GameRoomRequest.Create request) {
        Member host = memberAdaptor.queryById(hostId);
        autoLeaveIfInRoom(host);
        GameRoom gameRoom = gameRoomService.createGameRoom(host, request);

        // member profile view 설정
        MemberProfileRedisAdaptor.MemberProfileView profileView = memberProfileRedisAdaptor.findProfile(host.getId());
        String markerImageUrl = profileView != null
                ? profileView.markerImageUrl()
                : (host.getEquippedMarkerImage() != null ? host.getEquippedMarkerImage().getImageUrl() : null);

        // redis 설정
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(host, markerImageUrl, true);
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

            ReconcileResult reconcileResult = autoLeaveRecoveryService.reconcileBeforeTransition(
                    host.getId(),
                    currentRoomId,
                    null,
                    "CREATE");

            if (reconcileResult.isFatalFailure()) {
                throw new GameRoomHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
            }

            host.leaveGameRoom();
            log.info("Auto-leave reconcile completed before create - MemberId: {}, PreviousRoomId: {}, Status: {}",
                    host.getId(), currentRoomId, reconcileResult.getStatus());
        }
    }

}
