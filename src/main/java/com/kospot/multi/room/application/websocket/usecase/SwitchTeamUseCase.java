package com.kospot.multi.room.application.websocket.usecase;

import com.kospot.multi.player.domain.vo.GameTeam;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;

import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class SwitchTeamUseCase {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;

    //todo race condition 해결
    public void execute(String roomId, GameRoomRequest.SwitchTeam request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal principal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        Long memberId = principal.getMemberId();
        GameTeam team = GameTeam.fromString(request.getTeam());
        gameRoomRedisService.switchTeam(roomId, memberId, team.name());
        gameRoomNotificationService.notifyPlayerListUpdated(roomId);
    }

}
