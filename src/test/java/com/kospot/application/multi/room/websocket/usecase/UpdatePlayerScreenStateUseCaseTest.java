package com.kospot.application.multi.room.websocket.usecase;

import com.kospot.common.exception.object.domain.WebSocketHandler;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.multi.room.application.websocket.usecase.UpdatePlayerScreenStateUseCase;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.domain.vo.MultiplayerScreenState;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;
import com.kospot.multi.room.presentation.dto.request.GameRoomRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdatePlayerScreenStateUseCaseTest {

    @Mock
    private GameRoomRedisService gameRoomRedisService;

    @Mock
    private GameRoomNotificationService gameRoomNotificationService;

    @InjectMocks
    private UpdatePlayerScreenStateUseCase updatePlayerScreenStateUseCase;

    @Test
    @DisplayName("최신 seq 업데이트는 delta 알림을 브로드캐스트한다")
    void execute_shouldNotifyDelta_whenUpdated() {
        String roomId = "100";
        Long memberId = 1L;
        GameRoomRequest.UpdateScreenState request = GameRoomRequest.UpdateScreenState.builder()
                .state(MultiplayerScreenState.RESULT)
                .clientSeq(3L)
                .clientTimestamp(1000L)
                .build();
        SimpMessageHeaderAccessor accessor = buildAccessor(memberId);

        GameRoomPlayerInfo updated = GameRoomPlayerInfo.builder()
                .memberId(memberId)
                .screenState(MultiplayerScreenState.RESULT)
                .screenStateSeq(3L)
                .screenStateUpdatedAt(2000L)
                .build();

        when(gameRoomRedisService.isPlayerInRoom(roomId, memberId)).thenReturn(true);
        when(gameRoomRedisService.updatePlayerScreenStateIfNewer(anyString(), anyLong(),
                eq(MultiplayerScreenState.RESULT),
                eq(3L),
                anyLong())).thenReturn(new GameRoomRedisService.ScreenStateUpdateResult(
                        GameRoomRedisService.ScreenStateUpdateStatus.UPDATED,
                        updated
                ));

        updatePlayerScreenStateUseCase.execute(roomId, request, accessor);

        verify(gameRoomNotificationService).notifyPlayerScreenStateUpdated(roomId, updated);
    }

    @Test
    @DisplayName("동일 seq 멱등 요청은 알림을 전송하지 않는다")
    void execute_shouldNotNotify_whenNoOp() {
        String roomId = "100";
        Long memberId = 1L;
        GameRoomRequest.UpdateScreenState request = GameRoomRequest.UpdateScreenState.builder()
                .state(MultiplayerScreenState.ROOM)
                .clientSeq(3L)
                .clientTimestamp(1000L)
                .build();
        SimpMessageHeaderAccessor accessor = buildAccessor(memberId);

        when(gameRoomRedisService.isPlayerInRoom(roomId, memberId)).thenReturn(true);
        when(gameRoomRedisService.updatePlayerScreenStateIfNewer(anyString(), anyLong(),
                eq(MultiplayerScreenState.ROOM),
                eq(3L),
                anyLong())).thenReturn(new GameRoomRedisService.ScreenStateUpdateResult(
                        GameRoomRedisService.ScreenStateUpdateStatus.NO_OP,
                        null
                ));

        updatePlayerScreenStateUseCase.execute(roomId, request, accessor);

        verify(gameRoomNotificationService, never()).notifyPlayerScreenStateUpdated(anyString(), org.mockito.ArgumentMatchers.any());
        verify(gameRoomNotificationService, never()).notifyPlayerListUpdated(anyString());
    }

    @Test
    @DisplayName("참여자가 아니면 상태 변경 요청을 거부한다")
    void execute_shouldThrowForbidden_whenPlayerNotInRoom() {
        String roomId = "100";
        Long memberId = 1L;
        GameRoomRequest.UpdateScreenState request = GameRoomRequest.UpdateScreenState.builder()
                .state(MultiplayerScreenState.ROOM)
                .clientSeq(3L)
                .clientTimestamp(1000L)
                .build();
        SimpMessageHeaderAccessor accessor = buildAccessor(memberId);

        when(gameRoomRedisService.isPlayerInRoom(roomId, memberId)).thenReturn(false);

        assertThrows(WebSocketHandler.class, () -> updatePlayerScreenStateUseCase.execute(roomId, request, accessor));
    }

    @Test
    @DisplayName("더 오래된 seq 요청은 drop되고 알림을 전송하지 않는다")
    void execute_shouldNotNotify_whenStale() {
        String roomId = "100";
        Long memberId = 1L;
        GameRoomRequest.UpdateScreenState request = GameRoomRequest.UpdateScreenState.builder()
                .state(MultiplayerScreenState.IN_GAME)
                .clientSeq(1L)
                .clientTimestamp(1000L)
                .build();
        SimpMessageHeaderAccessor accessor = buildAccessor(memberId);

        when(gameRoomRedisService.isPlayerInRoom(roomId, memberId)).thenReturn(true);
        when(gameRoomRedisService.updatePlayerScreenStateIfNewer(anyString(), anyLong(),
                eq(MultiplayerScreenState.IN_GAME),
                eq(1L),
                anyLong())).thenReturn(new GameRoomRedisService.ScreenStateUpdateResult(
                        GameRoomRedisService.ScreenStateUpdateStatus.STALE,
                        null
                ));

        updatePlayerScreenStateUseCase.execute(roomId, request, accessor);

        verify(gameRoomNotificationService, never()).notifyPlayerScreenStateUpdated(anyString(), org.mockito.ArgumentMatchers.any());
        verify(gameRoomNotificationService, never()).notifyPlayerListUpdated(anyString());
    }

    @Test
    @DisplayName("인증 컨텍스트가 없으면 요청을 거부한다")
    void execute_shouldThrowUnauthorized_whenNoPrincipal() {
        String roomId = "100";
        GameRoomRequest.UpdateScreenState request = GameRoomRequest.UpdateScreenState.builder()
                .state(MultiplayerScreenState.ROOM)
                .clientSeq(1L)
                .clientTimestamp(1000L)
                .build();
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();

        assertThrows(WebSocketHandler.class, () -> updatePlayerScreenStateUseCase.execute(roomId, request, accessor));
    }

    private SimpMessageHeaderAccessor buildAccessor(Long memberId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        HashMap<String, Object> attributes = new HashMap<>();
        attributes.put("user", new WebSocketMemberPrincipal(memberId, "nick", "mail@test.com", "USER"));
        accessor.setSessionAttributes(attributes);
        return accessor;
    }
}
