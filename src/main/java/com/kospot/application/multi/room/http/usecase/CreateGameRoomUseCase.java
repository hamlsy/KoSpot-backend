package com.kospot.application.multi.room.http.usecase;


import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multi.room.entity.GameRoom;
import com.kospot.domain.multi.room.service.GameRoomService;
import com.kospot.domain.multi.room.vo.GameRoomPlayerInfo;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.multi.room.service.GameRoomRedisService;
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

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomService gameRoomService;

    public GameRoomResponse execute(Member host, GameRoomRequest.Create request) {
        GameRoom gameRoom = gameRoomService.createGameRoom(host, request);
        // redis 설정
        GameRoomPlayerInfo playerInfo = GameRoomPlayerInfo.from(host, true);
        gameRoomRedisService.addPlayerToRoom(gameRoom.getId().toString(), playerInfo);
        
        return GameRoomResponse.from(gameRoom);
    }

}
