package com.kospot.infrastructure.websocket.domain.multi.room.constants;

import static com.kospot.infrastructure.websocket.constants.WebSocketChannelConstants.PREFIX_TOPIC;

public class GameRoomChannelConstants {
    private GameRoomChannelConstants() {
        // Prevent instantiation
    }

    // ==================== 게임방 채널 ====================
    public static final String PREFIX_GAME_ROOM = PREFIX_TOPIC + "room/";

    /**
     * 게임방 플레이어 목록 채널 생성
     */
    public static String getGameRoomPlayerListChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME_ROOM + roomId + "/playerList";
    }

    /**
     * 게임방 채팅 채널 생성
     */
    public static String getGameRoomChatChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME_ROOM + roomId + "/chat";
    }

    /**
     * 게임방 설정 채널 생성
     */
    public static String getGameRoomSettingsChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME_ROOM + roomId + "/settings";
    }

    /**
     * 게임방 게임 시작 채널 생성
     */
    public static String getRoomGameStartChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME_ROOM + roomId + "/game-start";
    }

    /**
     * 게임방 상태 채널 생성
     */
    public static String getGameRoomStatusChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME_ROOM + roomId + "/status";
    }

    private static void validateId(String id, String paramName) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }

        // 특수 문자 검증 (경로에 문제가 될 수 있는 문자들)
        if (id.contains("/") || id.contains("?") || id.contains("#")) {
            throw new IllegalArgumentException(paramName + " contains invalid characters: " + id);
        }
    }
}
