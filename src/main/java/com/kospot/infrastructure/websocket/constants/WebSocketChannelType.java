package com.kospot.infrastructure.websocket.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WebSocket 채널 타입 정의
 * 새로운 채널 타입 추가 시 이 Enum에만 추가하면 됨
 */
@Getter
@RequiredArgsConstructor
public enum WebSocketChannelType {
    GLOBAL_CHAT("/topic/chat/", "전역 채팅", "글로벌 로비 채팅"),
    GAME_ROOM("/topic/room/", "게임방", "게임방 내 실시간 통신"),
    PERSONAL_MESSAGE("/user/", "개인 메시지", "Spring WebSocket 개인 메시지"),
    GLOBAL_NOTIFICATION("/topic/notification/", "전역 알림", "시스템 공지사항 등");

    private final String prefix;
    private final String displayName;
    private final String description;

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
                .map(type -> String.format("%s: %s (%s)", 
                    type.name(), type.getPrefix(), type.getDescription()))
                .collect(Collectors.joining(", "));
    }
}