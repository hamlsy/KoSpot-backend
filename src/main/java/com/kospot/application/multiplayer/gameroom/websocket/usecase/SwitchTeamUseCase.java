package com.kospot.application.multiplayer.gameroom.websocket.usecase;

import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomNotificationService;
import com.kospot.infrastructure.websocket.domain.gameroom.service.GameRoomRedisService;
import com.kospot.presentation.multigame.gameroom.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SwitchTeamUseCase {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;

    //todo team 검증
    public void execute(String roomId, GameRoomRequest.SwitchTeam request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal principal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        Long memberId = principal.getMemberId();

        gameRoomRedisService.switchTeam(roomId, memberId, request.getTeam());
        gameRoomNotificationService.notifyPlayerListUpdated(roomId, memberId.toString(), request.getTeam());
        log.info("Player switched team - MemberId: {}, RoomId: {}, NewTeam: {}", memberId, roomId, newTeam);
    }

}
