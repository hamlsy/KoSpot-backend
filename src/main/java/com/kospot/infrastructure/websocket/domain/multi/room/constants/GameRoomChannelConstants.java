package com.kospot.infrastructure.websocket.domain.multi.room.constants;

import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.PREFIX_TOPIC;
import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.validateId;

/**
 * 게임방 관련 WebSocket 채널 상수
 */
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
     * 게임방 상태 채널 생성
     */
    public static String getGameRoomStatusChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME_ROOM + roomId + "/status";
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 채널 경로가 특정 게임방에 속하는지 확인
     */
    public static boolean isGameRoomChannel(String destination, String roomId) {
        if (destination == null || roomId == null) {
            return false;
        }
        return destination.startsWith(PREFIX_GAME_ROOM + roomId + "/");
    }

    /**
     * 채널 경로에서 게임방 ID 추출
     */
    public static String extractRoomIdFromDestination(String destination) {
        if (destination == null || !destination.startsWith(PREFIX_GAME_ROOM)) {
            return null;
        }

        String remaining = destination.substring(PREFIX_GAME_ROOM.length());
        int slashIndex = remaining.indexOf('/');

        return slashIndex > 0 ? remaining.substring(0, slashIndex) : null;
    }
}
