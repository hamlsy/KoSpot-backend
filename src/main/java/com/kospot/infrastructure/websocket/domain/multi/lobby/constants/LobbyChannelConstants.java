package com.kospot.infrastructure.websocket.domain.multi.lobby.constants;

import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.PREFIX_TOPIC;

/**
 * 로비 관련 WebSocket 채널 상수
 */
public class LobbyChannelConstants {

    private LobbyChannelConstants() {
        // Prevent instantiation
    }

    // ==================== 로비 채팅 채널 ====================
    public static final String PREFIX_CHAT = PREFIX_TOPIC + "chat/";
    public static final String GLOBAL_LOBBY_CHANNEL = PREFIX_CHAT + "lobby";

    // 여기는 로비에서 방 목록을 수신받는 채널이다.
    public static final String ROOM_LIST_CHANNEL = PREFIX_TOPIC + "rooms";


}
