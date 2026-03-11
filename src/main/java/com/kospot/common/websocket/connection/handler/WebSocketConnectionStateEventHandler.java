package com.kospot.common.websocket.connection.handler;

import com.kospot.common.websocket.connection.event.WebSocketGracePeriodExpiredEvent;
import com.kospot.multi.room.application.usecase.LeaveGameRoomUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketConnectionStateEventHandler {

    private final LeaveGameRoomUseCase leaveGameRoomUseCase;

    @Async
    @EventListener
    public void handleGracePeriodExpired(WebSocketGracePeriodExpiredEvent event) {
        Long memberId = event.memberId();
        Long gameRoomId = event.gameRoomId();

        try {
            leaveGameRoomUseCase.execute(memberId, gameRoomId);
            log.info("Grace expired; member left game room - MemberId: {}, RoomId: {}", memberId, gameRoomId);
        } catch (Exception e) {
            log.warn("Failed to finalize room leave after grace expiration - MemberId: {}, RoomId: {}",
                    memberId, gameRoomId, e);
        }
    }
}
