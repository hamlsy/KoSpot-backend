package com.kospot.common.redis.common.constants;

public final class RedisKeyConstants {

    private RedisKeyConstants() {
        // Prevent instantiation
    }

    // redis
    public static final String REDIS_LOBBY_USERS = "lobby:users";
    public static final String REDIS_RECENT_CHAT_KEY = "chat:recent:";
    public static final String TRANSIENT_GLOBAL_LOBBY_CHAT_KEY = "chat:transient:lobby:global";
    public static final String TRANSIENT_GAME_ROOM_CHAT_KEY_PATTERN = "chat:transient:room:%s";
    public static final String TRANSIENT_GLOBAL_GAME_CHAT_KEY_PATTERN = "chat:transient:game:%s:global";

    // Main page cache keys
    public static final String ACTIVE_BANNERS_KEY = "main:active-banners";
    public static final String RECENT_NOTICES_KEY = "main:recent-notices";

    // Notification keys
    public static final String NOTIFICATION_SEQ_KEY = "notification:seq";
    public static final String NOTIFICATION_ITEM_KEY_PATTERN = "notification:item:%s";
    public static final String NOTIFICATION_USER_INDEX_KEY_PATTERN = "notification:user:%s:index";
    public static final String NOTIFICATION_USER_UNREAD_KEY_PATTERN = "notification:user:%s:unread";

    // Friend cache keys
    public static final String FRIEND_LIST_KEY_PATTERN = "friend:list:%s:v1";
    public static final String FRIEND_INCOMING_KEY_PATTERN = "friend:incoming:%s:v1";

    // Daily MVP cache keys
    public static final String DAILY_MVP_KEY_PATTERN = "mvp:daily:%s:v1";
    public static final String DAILY_MVP_NONE_KEY_PATTERN = "mvp:daily:none:%s:v1";
    public static final String DAILY_MVP_REBUILD_LOCK_KEY_PATTERN = "mvp:daily:rebuild:lock:%s";
    public static final String DAILY_MVP_SCHEDULE_LOCK_KEY_PATTERN = "mvp:daily:schedule:lock:%s";

}
