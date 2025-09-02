package com.kospot.application.multiplayer.gameroom.websocket.usecase;

import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.service.SessionContextRedisService;
import com.kospot.infrastructure.websocket.auth.WebSocketMemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@UseCase
@RequiredArgsConstructor
public class SetGameRoomIdAttrUseCase {

    private final SessionContextRedisService sessionContextRedisService;

    public void execute(String roomId, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal webSocketMemberPrincipal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        Long memberId = webSocketMemberPrincipal.getMemberId();
        String sessionId = headerAccessor.getSessionId();
        sessionContextRedisService.setAttr(sessionId, "roomId", roomId);
        sessionContextRedisService.setAttr(sessionId, "memberId", String.valueOf(memberId));
    }

}
