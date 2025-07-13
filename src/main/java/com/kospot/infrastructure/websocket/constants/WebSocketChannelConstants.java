package com.kospot.infrastructure.websocket.constants;

public final class WebSocketChannelConstants {
    private WebSocketChannelConstants() {

    }

    //prefix
    private static final String PREFIX_TOPIC = "/topic/";

    //global lobby
    public static final String PREFIX_CHAT = PREFIX_TOPIC + "chat/"; //prefix

    public static final String GLOBAL_LOBBY_CHANNEL = "lobby"; //todo 형식 변경

    //game room
    public static final String PREFIX_GAME_ROOM = PREFIX_TOPIC + "room/"; //prefix

    public static final String GAME_ROOM_PLAYERS = PREFIX_GAME_ROOM + "%s/players";
    public static final String GAME_ROOM_PLAYER_LIST = PREFIX_GAME_ROOM + "%s/playerList";
    public static final String GAME_ROOM_CHAT = PREFIX_GAME_ROOM + "%s/chat";
    public static final String GAME_ROOM_SETTINGS = PREFIX_GAME_ROOM + "%s/settings";

    //rate limit
    public static final int RATE_LIMIT = 40; // 1분에 허용되는 메시지 수
    public static final String RATE_LIMIT_KEY = "rate_limit:chat:";
}
