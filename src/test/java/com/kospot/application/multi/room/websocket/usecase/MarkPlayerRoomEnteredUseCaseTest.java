package com.kospot.application.multi.room.websocket.usecase;

import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.multi.room.application.websocket.usecase.MarkPlayerRoomEnteredUseCase;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.domain.vo.MultiplayerScreenState;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarkPlayerRoomEnteredUseCaseTest {

    @Mock
    private GameRoomRedisService gameRoomRedisService;

    @Mock
    private GameRoomNotificationService gameRoomNotificationService;

    @InjectMocks
    private MarkPlayerRoomEnteredUseCase markPlayerRoomEnteredUseCase;

    @Test
    @DisplayName("JOINING 플레이어 ROOM 승격 시 delta 알림을 보낸다")
    void execute_shouldNotifyDelta_whenUpdated() {
        String roomId = "10";
        Long memberId = 1L;
        SimpMessageHeaderAccessor accessor = buildAccessor(memberId);
        GameRoomPlayerInfo updatedPlayer = GameRoomPlayerInfo.builder()
                .memberId(memberId)
                .screenState(MultiplayerScreenState.ROOM)
                .screenStateSeq(1L)
                .build();

        when(gameRoomRedisService.promotePlayerToRoomIfJoining(eq(roomId), eq(memberId), anyLong()))
                .thenReturn(new GameRoomRedisService.ScreenStateUpdateResult(
                        GameRoomRedisService.ScreenStateUpdateStatus.UPDATED,
                        updatedPlayer
                ));

        markPlayerRoomEnteredUseCase.execute(roomId, accessor);

        verify(gameRoomNotificationService).notifyPlayerScreenStateUpdated(roomId, updatedPlayer);
    }

    @Test
    @DisplayName("이미 ROOM 상태이면 추가 알림을 보내지 않는다")
    void execute_shouldNotNotify_whenNoOp() {
        String roomId = "10";
        Long memberId = 1L;
        SimpMessageHeaderAccessor accessor = buildAccessor(memberId);

        when(gameRoomRedisService.promotePlayerToRoomIfJoining(eq(roomId), eq(memberId), anyLong()))
                .thenReturn(new GameRoomRedisService.ScreenStateUpdateResult(
                        GameRoomRedisService.ScreenStateUpdateStatus.NO_OP,
                        null
                ));

        markPlayerRoomEnteredUseCase.execute(roomId, accessor);

        verify(gameRoomNotificationService, never()).notifyPlayerScreenStateUpdated(eq(roomId), org.mockito.ArgumentMatchers.any());
        verify(gameRoomNotificationService, never()).notifyPlayerListUpdated(roomId);
    }

    private SimpMessageHeaderAccessor buildAccessor(Long memberId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        HashMap<String, Object> attrs = new HashMap<>();
        attrs.put("user", new WebSocketMemberPrincipal(memberId, "nick", "mail@test.com", "USER"));
        accessor.setSessionAttributes(attrs);
        return accessor;
    }
}
