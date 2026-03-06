package com.kospot.notification.domain.vo;

public enum NotificationType {

    ADMIN_MESSAGE("관리자 메시지"),
    NOTICE("공지사항"),
    FRIEND_REQUEST("친구 요청"),
    MVP_REWARD("오늘의 MVP 보상");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
