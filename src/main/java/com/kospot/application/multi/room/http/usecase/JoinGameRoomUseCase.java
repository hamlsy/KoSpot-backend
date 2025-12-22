package com.kospot.application.multi.room.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.event.GameRoomJoinEvent;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.infrastructure.redis.domain.member.service.MemberProfileRedisService;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.presentation.multi.room.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class JoinGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomRedisService gameRoomRedisService;
    private final MemberProfileRedisService memberProfileRedisService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    private final ApplicationEventPublisher eventPublisher;

    public void executeV1(Member player, Long gameRoomId, GameRoomRequest.Join request) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        validateGameCapacityV1(gameRoom);
        gameRoomService.joinGameRoom(player, gameRoom, request);
        
        // member profile view 설정
        if(memberProfileRedisAdaptor.findProfile(player.getId()) == null) {
            memberProfileRedisService.saveProfile(
                    player.getId(),
                    player.getNickname(),
                    player.getEquippedMarkerImage().getImageUrl()
            );
        }
        
        // GameRoom Redis 저장
        boolean isHost = gameRoom.isHost(player);
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.builder()
                .memberId(player.getId())
                .markerImageUrl(player.getEquippedMarkerImage().getImageUrl())
                .isHost(isHost)
                .nickname(player.getNickname())
//                .team(request.getTeam())
                .joinedAt(System.currentTimeMillis())
                .build();
        gameRoomRedisService.savePlayerToRoom(gameRoom.getId().toString(), playerInfo);
        
        // 알림용 이벤트 발행 (Redis 작업 완료 후)
        //todo team 랜덤 배정
        eventPublisher.publishEvent(new GameRoomJoinEvent(
                gameRoomId, player.getId(), player.getNickname(), player.getEquippedMarkerImage().getImageUrl(), null, isHost
        ));
    }

    // Read - then - check - V1 todo refactor
    private void validateGameCapacityV1(GameRoom gameRoom) {
        if(cannotJoinRoom(gameRoom)) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_FULL);
        }
    }

    private boolean cannotJoinRoom(GameRoom gameRoom) {
        return gameRoomRedisService.cannotJoinRoom(gameRoom.getId().toString(), gameRoom.getMaxPlayers());
    }

}
