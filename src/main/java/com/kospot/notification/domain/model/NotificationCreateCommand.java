package com.kospot.notification.domain.model;

import com.kospot.notification.domain.vo.NotificationType;

public record NotificationCreateCommand(
        Long receiverMemberId,
        NotificationType type,
        String title,
        String content,
        String payloadJson,
        Long sourceId
) {
}
