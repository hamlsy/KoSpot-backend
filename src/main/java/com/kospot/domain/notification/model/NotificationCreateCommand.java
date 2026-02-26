package com.kospot.domain.notification.model;

import com.kospot.domain.notification.vo.NotificationType;

public record NotificationCreateCommand(
        Long receiverMemberId,
        NotificationType type,
        String title,
        String content,
        String payloadJson,
        Long sourceId
) {
}
