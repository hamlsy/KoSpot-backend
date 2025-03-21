package com.kospot.kospot.application.multiplay.gameRoom;


import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.service.GameRoomService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.multiplay.gameRoom.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class CreateGameRoomUseCase {

    private final GameRoomService gameRoomService;

    public GameRoom execute(Member host, GameRoomRequest.Create request) {
        return gameRoomService.createGameRoom(host, request);
    }

}
