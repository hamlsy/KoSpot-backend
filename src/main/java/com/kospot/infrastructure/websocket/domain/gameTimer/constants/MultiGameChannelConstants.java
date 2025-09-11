package com.kospot.infrastructure.websocket.domain.gameTimer.constants;

public class MultiGameChannelConstants {
    private MultiGameChannelConstants() {
        // Prevent instantiation
    }

    // ==================== 공통 접두사 ====================
    public static final String PREFIX_TOPIC = "/topic/";
    public static final String PREFIX_GAME = PREFIX_TOPIC + "game/"; // /topic/game/{roomId}/

    // ==================== 공용 게임 내 채널 ====================
    public static String getTimerChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/timer";
    }

    public static String getPlayerStateChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/player";
    }

    // ==================== 로드뷰 게임 채널 ====================
    public static String getRoadViewSubmitChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/roadview/submit";
    }

    /**
     * 팀전 마커 채널
     * teamId 기준으로 같은 팀원에게만 브로드캐스트 가능
     */
    public static String getRoadViewTeamMarkerChannel(String roomId, String teamId) {
        validateId(roomId, "roomId");
        validateId(teamId, "teamId");
        return PREFIX_GAME + roomId + "/roadview/team/" + teamId + "/marker";
    }

    // ==================== 포토게임 채널 ====================
    public static String getPhotoSubmitChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/photo/submit";
    }

    public static String getPhotoAnswerChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/photo/answer";
    }

    // ==================== 유틸 ====================
    private static void validateId(String id, String name) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be null or blank");
        }
    }
}
