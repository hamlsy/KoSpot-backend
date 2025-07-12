package com.kospot.application.multiplayer.gameroom;


import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multigame.gameRoom.dto.request.GameRoomRequest;
import com.kospot.presentation.multigame.gameRoom.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CreateGameRoomUseCase {

    private final GameRoomService gameRoomService;

    public GameRoomResponse execute(Member host, GameRoomRequest.Create request) {
        GameRoom gameRoom = gameRoomService.createGameRoom(host, request);
        return GameRoomResponse.from(gameRoom);
    }

}
