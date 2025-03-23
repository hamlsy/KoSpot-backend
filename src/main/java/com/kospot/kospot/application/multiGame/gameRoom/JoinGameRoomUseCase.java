package com.kospot.kospot.application.multiGame.gameRoom;

import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.multiGame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.kospot.domain.multiGame.gameRoom.entity.GameRoom;
import com.kospot.kospot.domain.multiGame.gameRoom.service.GameRoomService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import com.kospot.kospot.presentation.multiGame.gameRoom.dto.request.GameRoomRequest;
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
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchPlayers(gameRoomId);
        gameRoomService.joinGameRoom(player, gameRoom, request);
    }

}
