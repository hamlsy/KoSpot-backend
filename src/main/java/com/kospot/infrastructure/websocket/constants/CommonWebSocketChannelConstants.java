package com.kospot.infrastructure.websocket.constants;

/**
 * WebSocket 채널 공통 상수 및 유틸리티 메서드
 * - 모든 도메인별 Channel Constants에서 공통으로 사용하는 필드와 메서드 제공
 * - static import를 통해 사용
 */
public final class CommonWebSocketChannelConstants {
    private CommonWebSocketChannelConstants() {
        // Utility class - prevent instantiation
    }

    // ==================== 기본 PREFIX ====================
    public static final String PREFIX_TOPIC = "/topic/";
    public static final String PREFIX_USER = "/user/";
    public static final String PREFIX_APP = "/app/";
    public static final String PREFIX_QUEUE = "/queue/";

    // ==================== 공통 유효성 검증 메서드 ====================

    /**
     * ID 유효성 검증
     * @param id 검증할 ID
     * @param paramName 파라미터 이름 (에러 메시지용)
     * @throws IllegalArgumentException ID가 null이거나 비어있거나, 특수 문자를 포함하는 경우
     */
    public static void validateId(String id, String paramName) {
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
     * @param memberId 검증할 멤버 ID
     * @throws IllegalArgumentException 멤버 ID가 null이거나 0 이하인 경우
     */
    public static void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new IllegalArgumentException("memberId must be a positive number");
        }
    }
}

