package com.kospot.application.multiGame.gameRoom;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.domain.multiGame.gameRoom.service.GameRoomService;
import com.kospot.global.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
import com.kospot.presentation.multiGame.gameRoom.dto.response.GameRoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;

    public GameRoomResponse execute(Member host, GameRoomRequest.Update request, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        return GameRoomResponse.from(gameRoomService.updateGameRoom(host, request, gameRoom));
    }

}
