package com.kospot.multi.room.application.websocket.usecase;

import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.exception.object.domain.WebSocketHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
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
public class UpdatePlayerScreenStateUseCase {

    private final GameRoomRedisService gameRoomRedisService;
    private final GameRoomNotificationService gameRoomNotificationService;

    public void execute(String roomId, GameRoomRequest.UpdateScreenState request, SimpMessageHeaderAccessor headerAccessor) {
        WebSocketMemberPrincipal principal = WebSocketMemberPrincipal.getPrincipal(headerAccessor);
        if (principal == null || principal.getMemberId() == null) {
            throw new WebSocketHandler(ErrorStatus._UNAUTHORIZED);
        }

        Long memberId = principal.getMemberId();
        if (!gameRoomRedisService.isPlayerInRoom(roomId, memberId)) {
            throw new WebSocketHandler(ErrorStatus._FORBIDDEN);
        }

        long serverTimestamp = System.currentTimeMillis();
        GameRoomRedisService.ScreenStateUpdateResult result = gameRoomRedisService.updatePlayerScreenStateIfNewer(
                roomId,
                memberId,
                request.getState(),
                request.getClientSeq(),
                serverTimestamp
        );

        switch (result.status()) {
            case UPDATED -> {
                if (result.playerInfo() != null) {
                    gameRoomNotificationService.notifyPlayerScreenStateUpdated(roomId, result.playerInfo());
                } else {
                    gameRoomNotificationService.notifyPlayerListUpdated(roomId);
                }
            }
            case NO_OP -> log.debug("Skip screen state update as idempotent - RoomId: {}, MemberId: {}, Seq: {}",
                    roomId, memberId, request.getClientSeq());
            case STALE -> log.debug("Drop stale screen state update - RoomId: {}, MemberId: {}, Seq: {}",
                    roomId, memberId, request.getClientSeq());
            case NOT_FOUND -> throw new WebSocketHandler(ErrorStatus.GAME_ROOM_PLAYER_NOT_FOUND);
        }
    }
}
