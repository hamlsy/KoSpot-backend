package com.kospot.infrastructure.redis.domain.multi.room.constants;

public class GameRoomRedisKeyConstants {

    // Redis Key Patterns
    private static final String ROOM_PLAYERS_KEY = "game:room:%s:players";
    private static final String PLAYER_SESSION_KEY = "game:player:%s:session";
    private static final String SESSION_SUBSCRIPTIONS_KEY = "game:session:%s:subscriptions";
    private static final String SESSION_ROOM_KEY = "game:session:%s:room";

    public static String getRoomKey(String roomId) {
        return String.format(ROOM_PLAYERS_KEY, roomId);
    }

    public static String getPlayerSessionKey(String playerId) {
        return String.format(PLAYER_SESSION_KEY, playerId);
    }


}
