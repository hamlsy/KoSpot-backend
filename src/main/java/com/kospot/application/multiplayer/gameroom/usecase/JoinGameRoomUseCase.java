package com.kospot.application.multiplayer.gameroom.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.event.GameRoomJoinEvent;
import com.kospot.domain.multigame.gameRoom.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.object.domain.GameRoomHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomPlayerService;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
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

    private final ApplicationEventPublisher eventPublisher;

    public void executeV1(Member player, Long gameRoomId, GameRoomRequest.Join request) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        validateGameCapacityV1(gameRoom);
        gameRoomService.joinGameRoom(player, gameRoom, request);
        eventPublisher.publishEvent(new GameRoomJoinEvent(gameRoom, player));
    }

    // Read - then - check - V1
    private void validateGameCapacityV1(GameRoom gameRoom) {
        if(!gameRoomRedisService.canJoinRoom(gameRoom.getId().toString(), gameRoom.getMaxPlayers())) {
            throw new GameRoomHandler(ErrorStatus.GAME_ROOM_IS_FULL);
        }
    }

}
