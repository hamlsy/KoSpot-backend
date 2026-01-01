package com.kospot.infrastructure.websocket.domain.notification.constants;

import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.PREFIX_TOPIC;
import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.PREFIX_USER;
import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.validateMemberId;

/**
 * 알림 관련 WebSocket 채널 상수
 * - 전역 알림 채널
 * - 개인 알림 채널
 */
public class NotificationChannelConstants {

    private NotificationChannelConstants() {
        // Prevent instantiation
    }

    // ==================== 전역 알림 채널 ====================
    public static final String PREFIX_NOTIFICATION = PREFIX_TOPIC + "notification/";
    public static final String GLOBAL_NOTIFICATION_CHANNEL = PREFIX_NOTIFICATION + "global";
    public static final String SYSTEM_MAINTENANCE_CHANNEL = PREFIX_NOTIFICATION + "maintenance";

    // ==================== 개인 메시지 채널 ====================

    /**
     * 개인 알림 채널 생성
     * @param memberId 멤버 ID
     * @return 개인 알림 채널 경로 (/user/{memberId}/notification)
     */
    public static String getPersonalNotificationChannel(Long memberId) {
        validateMemberId(memberId);
        return PREFIX_USER + memberId + "/notification";
    }

    /**
     * 개인 게임 초대 채널 생성
     * @param memberId 멤버 ID
     * @return 개인 게임 초대 채널 경로 (/user/{memberId}/gameInvite)
     */
    public static String getPersonalGameInviteChannel(Long memberId) {
        validateMemberId(memberId);
        return PREFIX_USER + memberId + "/gameInvite";
    }

    // ==================== 유틸리티 메서드 ====================

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
}

