package com.kospot.application.multi.room.http.usecase;

import com.kospot.domain.multi.room.adaptor.GameRoomAdaptor;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
import com.kospot.presentation.multi.gameroom.dto.response.GameRoomDetailResponse;
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
