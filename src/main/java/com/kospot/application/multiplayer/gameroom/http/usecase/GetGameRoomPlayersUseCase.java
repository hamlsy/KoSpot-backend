package com.kospot.application.multiplayer.gameroom.http.usecase;

import com.kospot.domain.multigame.gameRoom.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.response.GameRoomPlayerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class GetGameRoomPlayersUseCase {

    private final GameRoomRedisService gameRoomRedisService;

    public List<GameRoomPlayerResponse> execute(Long gameRoomId) {
        List<GameRoomPlayerInfo> players = gameRoomRedisService.getRoomPlayers(gameRoomId.toString());
        return players.stream().map(GameRoomPlayerInfo::toResponse).toList();
    }

}
