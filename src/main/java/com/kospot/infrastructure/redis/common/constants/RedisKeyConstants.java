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

    // Notification keys
    public static final String NOTIFICATION_SEQ_KEY = "notification:seq";
    public static final String NOTIFICATION_ITEM_KEY_PATTERN = "notification:item:%s";
    public static final String NOTIFICATION_USER_INDEX_KEY_PATTERN = "notification:user:%s:index";
    public static final String NOTIFICATION_USER_UNREAD_KEY_PATTERN = "notification:user:%s:unread";

}
