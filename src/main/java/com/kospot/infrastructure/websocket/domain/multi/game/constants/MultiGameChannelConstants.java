package com.kospot.infrastructure.websocket.domain.multi.game.constants;

import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.PREFIX_TOPIC;
import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.validateId;

/**
 * 멀티게임 관련 WebSocket 채널 상수
 */
public class MultiGameChannelConstants {
    private MultiGameChannelConstants() {
        // Prevent instantiation
    }

    // ==================== 공통 접두사 ====================
    public static final String PREFIX_GAME = PREFIX_TOPIC + "game/"; // /topic/game/{roomId}/

    // ==================== 공용 게임 내 채널 ====================
    public static String getStartGameChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/start";
    }

    public static String getTimerChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/timer";
    }

    public static String getPlayerStateChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/player";
    }

    public static String getIntroChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/intro";
    }

    public static String getLoadingStatusChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/loading/status";
    }

    // ==================== 로드뷰 게임 채널 ====================

    /**
     * 로드뷰 개인전 제출 알림 채널
     * 용도: 누가 제출했는지 실시간 알림
     */
     public static String getRoadViewPlayerSubmissionChannel(String gameId) {
        validateId(gameId, "gameId");
        return PREFIX_GAME + gameId + "/roadview/submissions/player";
    }

    /**
     * 로드뷰 팀전 제출 알림 채널
     * 용도: 팀원이 제출했는지 실시간 알림 (같은 팀만)
     */
    public static String getRoadViewTeamSubmissionChannel(String gameId, String teamId) {
        validateId(gameId, "gameId");
        validateId(teamId, "teamId");
        return PREFIX_GAME + gameId + "/roadview/submissions/team/" + teamId;
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

    // ==================== 라운드 관련 채널 ====================
    
    /**
     * 라운드 결과 알림 채널
     * 용도: 라운드 종료 시 점수, 순위 등 결과 브로드캐스트
     */
    public static String getRoundResultChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/round/result";
    }

    /**
     * 라운드 시작 알림 채널
     * 용도: 새 라운드 시작 알림
     */
    public static String getRoundStartChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/round/start";
    }

    /**
     * 라운드 전환 대기 타이머 채널
     * 용도: 라운드 종료 후 다음 라운드까지 대기 시간 동기화 (10초)
     */
    public static String getRoundTransitionChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/round/transition";
    }

    /**
     * 게임 종료 알림
     */
    public static String getGameFinishChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/game/finished";
    }
    
    // ==================== 채팅 채널 ===============
    public static String getGlobalChatChannel(String roomId) {
        validateId(roomId, "roomId");
        return PREFIX_GAME + roomId + "/chat/global";
    }

    public static String getTeamChatChannel(String roomId, String teamId) {
        validateId(roomId, "roomId");
        validateId(teamId, "teamId");
        return PREFIX_GAME + roomId + "/chat/team/" + teamId;
    }

    // ==================== 유틸리티 메서드 ====================

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
}
