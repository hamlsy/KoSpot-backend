package com.kospot.application.multi.room.http.usecase;


import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multi.gameroom.dto.request.GameRoomRequest;
import com.kospot.presentation.multi.gameroom.dto.response.GameRoomResponse;
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
        host.joinGameRoom(gameRoom.getId());
        return GameRoomResponse.from(gameRoom);
    }

}
