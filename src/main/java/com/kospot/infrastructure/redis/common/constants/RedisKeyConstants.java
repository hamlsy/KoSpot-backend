package com.kospot.infrastructure.redis.common.constants;

public final class RedisKeyConstants {

    private RedisKeyConstants() {
        // Prevent instantiation
    }

    // redis
    public static final String REDIS_LOBBY_USERS = "lobby:users";
    public static final String REDIS_RECENT_CHAT_KEY = "chat:recent:";

    // Main page cache keys
    public static final String ACTIVE_BANNERS_KEY = "main:active-banners";
    public static final String RECENT_NOTICES_KEY = "main:recent-notices";

}
