package com.kospot.domain.notification.model;

import com.kospot.domain.notification.vo.NotificationType;

import java.time.LocalDateTime;

public record NotificationData(
        Long notificationId,
        Long receiverMemberId,
        NotificationType type,
        String title,
        String content,
        String payloadJson,
        Long sourceId,
        boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
