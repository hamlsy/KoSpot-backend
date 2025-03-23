package com.kospot.kospot.application.multiGame.gameRoom;


import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiGame.gameRoom.service.GameRoomService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.response.GameRoomResponse;
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
