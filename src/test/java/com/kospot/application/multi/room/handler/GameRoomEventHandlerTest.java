package com.kospot.application.multi.room.handler;

import com.kospot.multi.room.application.handler.GameRoomEventHandler;
import com.kospot.multi.room.domain.event.GameRoomJoinEvent;
import com.kospot.multi.room.domain.vo.GameRoomPlayerInfo;
import com.kospot.multi.room.infrastructure.redis.service.GameRoomRedisService;
import com.kospot.multi.room.infrastructure.websocket.service.GameRoomNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameRoomEventHandlerTest {

    @Mock
    private GameRoomNotificationService gameRoomNotificationService;

    @Mock
    private GameRoomRedisService gameRoomRedisService;

    @InjectMocks
    private GameRoomEventHandler gameRoomEventHandler;

    @Test
    @DisplayName("join 알림은 Redis 저장값을 조회해 전송한다")
    void handleJoin_shouldSendJoinedAndListUpdated_whenPlayerExistsInRedis() {
        String roomId = "10";
        Long memberId = 100L;
        GameRoomJoinEvent event = org.mockito.Mockito.mock(GameRoomJoinEvent.class);
        when(event.getRoomId()).thenReturn(Long.valueOf(roomId));
        when(event.getMemberId()).thenReturn(memberId);

        GameRoomPlayerInfo storedPlayerInfo = org.mockito.Mockito.mock(GameRoomPlayerInfo.class);

        when(gameRoomRedisService.getRoomPlayer(roomId, memberId)).thenReturn(Optional.of(storedPlayerInfo));

        gameRoomEventHandler.handleJoin(event);

        verify(gameRoomNotificationService).notifyPlayerJoined(roomId, storedPlayerInfo);
        verify(gameRoomNotificationService).notifyPlayerListUpdated(roomId);
    }

    @Test
    @DisplayName("Redis에 플레이어 정보가 없으면 delta 알림 없이 전체 동기화만 전송한다")
    void handleJoin_shouldSendOnlyListUpdated_whenPlayerMissingInRedis() {
        String roomId = "10";
        Long memberId = 100L;
        GameRoomJoinEvent event = org.mockito.Mockito.mock(GameRoomJoinEvent.class);
        when(event.getRoomId()).thenReturn(Long.valueOf(roomId));
        when(event.getMemberId()).thenReturn(memberId);

        when(gameRoomRedisService.getRoomPlayer(roomId, memberId)).thenReturn(Optional.empty());

        gameRoomEventHandler.handleJoin(event);

        verify(gameRoomNotificationService, never()).notifyPlayerJoined(any(), any());
        verify(gameRoomNotificationService).notifyPlayerListUpdated(roomId);
    }
}
