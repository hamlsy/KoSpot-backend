package com.kospot.common.websocket.connection.event;

public record WebSocketGracePeriodExpiredEvent(Long memberId, Long gameRoomId) {
}
