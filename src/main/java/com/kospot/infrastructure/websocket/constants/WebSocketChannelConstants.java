package com.kospot.infrastructure.websocket.constants;

public final class WebSocketChannelConstants {
    private WebSocketChannelConstants() {

    }
    //prefix
    private static final String PREFIX_TOPIC = "/topic/";
    public static final String PREFIX_CHAT = PREFIX_TOPIC + "chat/";

    //channel
    public static final String GLOBAL_LOBBY_CHANNEL = "lobby";

    //rate limit
    public static final int RATE_LIMIT = 40; // 1분에 허용되는 메시지 수
    public static final String RATE_LIMIT_KEY = "rate_limit:chat:";
}
