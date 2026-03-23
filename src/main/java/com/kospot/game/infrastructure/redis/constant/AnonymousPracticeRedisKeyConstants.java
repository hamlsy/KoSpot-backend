package com.kospot.game.infrastructure.redis.constant;

public class AnonymousPracticeRedisKeyConstants {

    private static final String TOKEN_KEY = "game:practice:anonymous:%s";

    public static String getTokenKey(Long gameId) {
        return String.format(TOKEN_KEY, gameId);
    }
}
