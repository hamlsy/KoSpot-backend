package com.kospot.application.multiplayer.gameroom.usecase;

import com.kospot.domain.member.adaptor.MemberAdaptor;
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
public class KickPlayerUseCase {

    private final GameRoomAdaptor gameRoomAdaptor;
    private final GameRoomService gameRoomService;
    private final MemberAdaptor memberAdaptor;
    private final GameRoomPlayerService gameRoomPlayerService;

    public void execute(Member host, GameRoomRequest.Kick request, Long gameRoomId) {
        GameRoom gameRoom = gameRoomAdaptor.queryById(gameRoomId);
        Member targetPlayer = memberAdaptor.queryById(request.getTargetPlayerId());
        
        // 데이터베이스 레벨에서 강퇴 처리
        gameRoomService.kickPlayer(host, targetPlayer, gameRoom);
        
        // WebSocket 레벨에서 실시간 강퇴 처리 (Redis + 실시간 알림)
        gameRoomPlayerService.kickPlayer(gameRoomId, host.getId(), targetPlayer.getId());
    }

}
