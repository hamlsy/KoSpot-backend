package com.kospot.kospot.application.multiplay.gameRoom;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiplay.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.kospot.domain.multiplay.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiplay.gameRoom.service.GameRoomService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.multiplay.gameRoom.dto.request.GameRoomRequest;
import com.kospot.kospot.presentation.multiplay.gameRoom.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class JoinGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;

    public void execute(Member player, Long gameRoomId, GameRoomRequest.Join request) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        gameRoomService.joinGameRoom(player, gameRoom, request);
    }

}
