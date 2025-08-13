package com.kospot.application.multiplayer.gameroom.http.usecase;

import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.response.GameRoomDetailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetGameRoomDetailUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomRedisService gameRoomRedisService;

    public GameRoomDetailResponse execute(Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(gameRoomId.toString());

        return GameRoomDetailResponse.from(gameRoom, players);
    }

}
