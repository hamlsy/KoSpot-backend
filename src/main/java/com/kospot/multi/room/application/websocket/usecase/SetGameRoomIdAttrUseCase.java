package com.kospot.multi.room.application.websocket.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.redis.common.service.SessionContextRedisService;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
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
