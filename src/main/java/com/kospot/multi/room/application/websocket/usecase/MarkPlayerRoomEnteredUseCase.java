package com.kospot.multi.room.application.websocket.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class MarkPlayerRoomEnteredUseCase {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;

    public void execute(String roomId, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal principal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        if (principal == null || principal.getMemberId() == null || principal.getMemberId() <= 0) {
            return;
        }

        Long memberId = principal.getMemberId();
        GameRoomRedisService.ScreenStateUpdateResult result = gameRoomRedisService.promotePlayerToRoomIfJoining(
                roomId,
                memberId,
                System.currentTimeMillis()
        );

        switch (result.status()) {
            case UPDATED -> {
                if (result.playerInfo() != null) {
                    gameRoomNotificationService.notifyPlayerScreenStateUpdated(roomId, result.playerInfo());
                } else {
                    gameRoomNotificationService.notifyPlayerListUpdated(roomId);
                }
            }
            case NO_OP -> log.debug("Skip room-entered promotion as no-op - RoomId: {}, MemberId: {}", roomId, memberId);
            case STALE -> log.debug("Skip room-entered promotion as stale - RoomId: {}, MemberId: {}", roomId, memberId);
            case NOT_FOUND -> log.debug("Skip room-entered promotion because player not found - RoomId: {}, MemberId: {}", roomId, memberId);
        }
    }
}
