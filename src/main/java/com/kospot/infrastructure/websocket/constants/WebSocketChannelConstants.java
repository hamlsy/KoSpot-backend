package com.kospot.infrastructure.websocket.constants;

import static com.kospot.infrastructure.websocket.domain.multi.room.constants.GameRoomChannelConstants.PREFIX_GAME_ROOM;

/**
 * WebSocket 채널 경로 및 설정 상수
 * - 구체적인 메시지 전송 경로 생성에 사용
 * - 동적 채널 경로 생성 및 유틸리티 제공
 */
public final class WebSocketChannelConstants {
    private WebSocketChannelConstants() {
        // Utility class - prevent instantiation
    }

    // ==================== 기본 PREFIX ====================
    public static final String PREFIX_TOPIC = "/topic/";
    public static final String PREFIX_USER = "/user/";

    // ==================== 글로벌 채팅 ====================
    public static final String PREFIX_CHAT = PREFIX_TOPIC + "chat/";
    public static final String GLOBAL_LOBBY_CHANNEL = PREFIX_CHAT + "lobby";


    // ==================== 게임 내부 채널 ====================
    public static final String PREFIX_GAME = PREFIX_TOPIC + "game/";

    /**
     * 게임 진행 상황 채널 생성
     */
    public static String getGameProgressChannel(String gameId) {
        validateId(gameId, "gameId");
        return PREFIX_GAME + gameId + "/progress";
    }

    /**
     * 게임 결과 채널 생성
     */
    public static String getGameResultChannel(String gameId) {
        validateId(gameId, "gameId");
        return PREFIX_GAME + gameId + "/result";
    }

    /**
     * 게임 답안 제출 채널 생성
     */
    public static String getGameSubmissionChannel(String gameId) {
        validateId(gameId, "gameId");
        return PREFIX_GAME + gameId + "/submission";
    }

    // ==================== 개인 메시지 ====================

    /**
     * 개인 알림 채널 생성
     */
    public static String getPersonalNotificationChannel(Long memberId) {
        validateMemberId(memberId);
        return PREFIX_USER + memberId + "/notification";
    }

    /**
     * 개인 게임 초대 채널 생성
     */
    public static String getPersonalGameInviteChannel(Long memberId) {
        validateMemberId(memberId);
        return PREFIX_USER + memberId + "/gameInvite";
    }

    // ==================== 전역 알림 ====================
    public static final String PREFIX_NOTIFICATION = PREFIX_TOPIC + "notification/";
    public static final String GLOBAL_NOTIFICATION_CHANNEL = PREFIX_NOTIFICATION + "global";
    public static final String SYSTEM_MAINTENANCE_CHANNEL = PREFIX_NOTIFICATION + "maintenance";

    // ==================== Rate Limiting 설정 ====================
    public static final int RATE_LIMIT = 40; // 1분에 허용되는 메시지 수
    public static final String RATE_LIMIT_KEY = "rate_limit:chat:";

    // ==================== 헬퍼 메서드 ====================

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

    /**
     * 채널 경로에서 게임 ID 추출
     */
    public static String extractGameIdFromDestination(String destination) {
        if (destination == null || !destination.startsWith(PREFIX_GAME)) {
            return null;
        }

        String remaining = destination.substring(PREFIX_GAME.length());
        int slashIndex = remaining.indexOf('/');

        return slashIndex > 0 ? remaining.substring(0, slashIndex) : null;
    }

    /**
     * 채널 경로에서 멤버 ID 추출 (/user/{memberId}/... 형식)
     */
    public static Long extractMemberIdFromDestination(String destination) {
        if (destination == null || !destination.startsWith(PREFIX_USER)) {
            return null;
        }

        try {
            String remaining = destination.substring(PREFIX_USER.length());
            int slashIndex = remaining.indexOf('/');

            String memberIdStr = slashIndex > 0 ? remaining.substring(0, slashIndex) : remaining;
            return Long.parseLong(memberIdStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ==================== 유효성 검증 ====================

    /**
     * ID 유효성 검증
     */
    private static void validateId(String id, String paramName) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(paramName + " cannot be null or empty");
        }

        // 특수 문자 검증 (경로에 문제가 될 수 있는 문자들)
        if (id.contains("/") || id.contains("?") || id.contains("#")) {
            throw new IllegalArgumentException(paramName + " contains invalid characters: " + id);
        }
    }

    /**
     * 멤버 ID 유효성 검증
     */
    private static void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be a positive number");
        }
    }
}