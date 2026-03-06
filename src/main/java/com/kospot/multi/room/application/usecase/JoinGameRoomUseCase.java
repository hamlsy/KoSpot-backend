package com.kospot.multi.room.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.multi.room.application.adaptor.GameRoomAdaptor;
import com.kospot.multi.room.domain.entity.GameRoom;
import com.kospot.multi.room.domain.event.GameRoomJoinEvent;
import com.kospot.multi.room.application.service.service.GameRoomService;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.exception.object.domain.GameRoomHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.member.infrastructure.redis.service.MemberProfileRedisService;

import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class JoinGameRoomUseCase {

    private final MemberAdaptor memberAdaptor;
    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final MemberProfileRedisService memberProfileRedisService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    private final ApplicationEventPublisher eventPublisher;
    private final LeaveGameRoomUseCase leaveGameRoomUseCase;

    public void executeV1(Long playerId, Long gameRoomId, GameRoomRequest.Join request) {
        Member player = memberAdaptor.queryById(playerId);
        autoLeaveIfInOtherRoom(player, gameRoomId);
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        validateGameCapacityV1(gameRoom);
        gameRoomService.joinGameRoom(player, gameRoom, request);

        // member profile view 설정
        if (memberProfileRedisAdaptor.findProfile(player.getId()) == null) {
            memberProfileRedisService.saveProfile(
                    player.getId(),
                    player.getNickname(),
                    player.getEquippedMarkerImage().getImageUrl());
        }

        // GameRoom Redis 저장
        boolean isHost = gameRoom.isHost(player);
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                .memberId(player.getId())
                .markerImageUrl(player.getEquippedMarkerImage().getImageUrl())
                .isHost(isHost)
                .nickname(player.getNickname())
                // .team(request.getTeam())
                .joinedAt(System.currentTimeMillis())
                .build();
        gameRoomRedisService.savePlayerToRoom(gameRoom.getId().toString(), playerInfo);

        // 알림용 이벤트 발행 (Redis 작업 완료 후)
        // todo team 랜덤 배정
        eventPublisher.publishEvent(new GameRoomJoinEvent(
                gameRoomId, player.getId(), player.getNickname(), player.getEquippedMarkerImage().getImageUrl(), null,
                isHost));
    }

    // Read - then - check - V1 todo refactor
    private void validateGameCapacityV1(GameRoom gameRoom) {
        if (cannotJoinRoom(gameRoom)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_FULL);
        }
    }

    private boolean cannotJoinRoom(GameRoom gameRoom) {
        return gameRoomRedisService.cannotJoinRoom(gameRoom.getId().toString(), gameRoom.getMaxPlayers());
    }

    private void autoLeaveIfInOtherRoom(Member player, Long targetRoomId) {
        if (player.isAlreadyInOtherGameRoom(targetRoomId)) {
            Long previousRoomId = player.getGameRoomId();
            log.info("Auto-leaving room {} before joining room {} - MemberId: {}",
                    previousRoomId, targetRoomId, player.getId());
            leaveGameRoomUseCase.execute(player.getId(), previousRoomId);
        }
    }

}
