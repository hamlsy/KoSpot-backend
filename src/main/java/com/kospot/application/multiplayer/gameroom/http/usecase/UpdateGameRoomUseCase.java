package com.kospot.application.multiplayer.gameroom.http.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import com.kospot.presentation.multigame.gameroom.dto.response.GameRoomResponse;
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
        GameRoom gameRoom = gameRoomAdaptor.queryByIdFetchHost(gameRoomId);
        return GameRoomResponse.from(gameRoomService.updateGameRoom(host, request, gameRoom));
    }

}
