package com.kospot.application.multiplayer.gameroom.usecase;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomPlayerService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
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
    private final GameRoomPlayerService gameRoomPlayerService;

    public void execute(Member player, Long gameRoomId, GameRoomRequest.Join request) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        // 데이터베이스 레벨에서 입장 처리
        gameRoomService.joinGameRoom(player, gameRoom, request);
        
        // 주의: 실시간 알림은 WebSocket 구독 시점에서 GameRoomSessionManager.addSubscription()을 통해 처리됩니다.

    }

}
