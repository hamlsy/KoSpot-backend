package com.kospot.infrastructure.websocket.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebSocket 채널 타입 정의 및 권한 관리
 * - 인터셉터에서 구독 권한 검증에 사용
 * - 새로운 채널 타입 추가 시 이 Enum에만 추가하면 됨
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketChannelType {
    GLOBAL_CHAT("/topic/chat/", "전역 채팅", "글로벌 로비 채팅", ChannelAccessLevel.PUBLIC),
    GAME_ROOM("/topic/room/", "게임방", "게임방 내 실시간 통신", ChannelAccessLevel.AUTHENTICATED),
    GAME_SESSION("/topic/game/", "게임 세션", "실제 게임 진행 중 통신", ChannelAccessLevel.AUTHENTICATED),
    PERSONAL_MESSAGE("/user/", "개인 메시지", "Spring WebSocket 개인 메시지", ChannelAccessLevel.PRIVATE),
    GLOBAL_NOTIFICATION("/topic/notification/", "전역 알림", "시스템 공지사항 등", ChannelAccessLevel.PUBLIC);

    /**
     * 채널 접근 권한 레벨
     */
    public enum ChannelAccessLevel {
        PUBLIC,        // 누구나 접근 가능
        AUTHENTICATED, // 인증된 사용자만
        PRIVATE        // 개인 전용
    }

    private final String prefix;
    private final String displayName;
    private final String description;
    private final ChannelAccessLevel accessLevel;

    /**
     * 모든 허용된 PREFIX 반환
     */
    public static Set<String> getAllowedPrefixes() {
        return Arrays.stream(values())
                .map(WebSocketChannelType::getPrefix)
                .collect(Collectors.toSet());
    }

    /**
     * 목적지가 유효한 채널 타입인지 확인
     */
    public static boolean isValidDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            return false;
        }
        
        return Arrays.stream(values())
                .anyMatch(type -> destination.startsWith(type.getPrefix()));
    }

    /**
     * 목적지에 해당하는 채널 타입 반환
     */
    public static Optional<WebSocketChannelType> fromDestination(String destination) {
        if (destination == null || destination.trim().isEmpty()) {
            return Optional.empty();
        }
        
        return Arrays.stream(values())
                .filter(type -> destination.startsWith(type.getPrefix()))
                .findFirst();
    }

    /**
     * 채널 타입 이름으로 조회
     */
    public static Optional<WebSocketChannelType> fromName(String name) {
        try {
            return Optional.of(WebSocketChannelType.valueOf(name.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * 디버깅을 위한 모든 채널 타입 정보 반환
     */
    public static String getAllChannelTypesInfo() {
        return Arrays.stream(values())
                .map(type -> String.format("%s: %s (%s) [%s]", 
                    type.name(), type.getPrefix(), type.getDescription(), type.getAccessLevel()))
                .collect(Collectors.joining(", "));
    }

    /**
     * 특정 접근 레벨에 해당하는 채널 타입들 반환
     */
    public static Set<WebSocketChannelType> getChannelsByAccessLevel(ChannelAccessLevel level) {
        return Arrays.stream(values())
                .filter(type -> type.getAccessLevel() == level)
                .collect(Collectors.toSet());
    }

    /**
     * 사용자가 해당 채널에 접근할 수 있는지 확인
     */
    public boolean canAccess(Long memberId, String specificDestination) {
        return switch (this.accessLevel) {
            case PUBLIC -> true;
            case AUTHENTICATED -> memberId != null;
            case PRIVATE -> memberId != null && isUserSpecificChannel(specificDestination, memberId);
        };
    }

    /**
     * 개인 전용 채널인지 확인 (예: /user/{memberId}/...)
     */
    private boolean isUserSpecificChannel(String destination, Long memberId) {
        if (this != PERSONAL_MESSAGE || destination == null || memberId == null) {
            return false;
        }
        return destination.contains("/user/" + memberId + "/");
    }
}