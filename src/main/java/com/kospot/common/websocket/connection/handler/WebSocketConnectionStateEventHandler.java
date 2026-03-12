package com.kospot.common.websocket.connection.handler;

import com.kospot.common.websocket.connection.event.WebSocketGracePeriodExpiredEvent;
import com.kospot.multi.room.application.service.RoomExitOrchestrator;
import com.kospot.multi.room.application.vo.LeaveRoomResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionStateEventHandler {

    private final RoomExitOrchestrator roomExitOrchestrator;

    @Async
    @EventListener
    public void handleGracePeriodExpired(WebSocketGracePeriodExpiredEvent event) {
        Long memberId = event.memberId();
        Long gameRoomId = event.gameRoomId();

        try {
            LeaveRoomResult leaveResult = roomExitOrchestrator.requestExit(
                    memberId,
                    gameRoomId,
                    "GRACE_EXPIRED",
                    "disconnect-grace-expired");
            log.info("Grace expired; leave finalized - MemberId: {}, RoomId: {}, Status: {}",
                    memberId, gameRoomId, leaveResult.getStatus());
        } catch (Exception e) {
            log.warn("Failed to finalize room leave after grace expiration - MemberId: {}, RoomId: {}",
                    memberId, gameRoomId, e);
        }
    }
}
