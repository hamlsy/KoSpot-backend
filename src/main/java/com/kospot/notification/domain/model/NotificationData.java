package com.kospot.notification.domain.model;

import com.kospot.notification.domain.vo.NotificationType;

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
