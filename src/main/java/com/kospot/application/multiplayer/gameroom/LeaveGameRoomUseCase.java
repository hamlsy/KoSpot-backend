package com.kospot.application.multiplayer.gameroom;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.multigame.gameRoom.adaptor.GameRoomAdaptor;
import com.kospot.domain.multigame.gameRoom.entity.GameRoom;
import com.kospot.domain.multigame.gameRoom.service.GameRoomService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.service.GameRoomPlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class LeaveGameRoomUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final GameRoomPlayerService gameRoomPlayerService;

    public void execute(Member player, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        
        // 데이터베이스 레벨에서 퇴장 처리
        gameRoomService.leaveGameRoom(player, gameRoom);
        
        // WebSocket 레벨에서 실시간 퇴장 처리 (Redis + 실시간 알림)
        gameRoomPlayerService.removePlayerFromRoom(gameRoomId, player.getId());
    }

}
