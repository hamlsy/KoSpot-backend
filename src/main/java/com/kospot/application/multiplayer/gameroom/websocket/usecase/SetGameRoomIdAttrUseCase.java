package com.kospot.application.multiplayer.gameroom.websocket.usecase;

import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.service.SessionContextRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@UseCase
@RequiredArgsConstructor
public class SetGameRoomIdAttrUseCase {

    private final SessionContextRedisService sessionContextRedisService;

    public void execute(String roomId, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        sessionContextRedisService.setAttr(sessionId, "roomId", roomId);
    }

}
