package com.kospot.presentation.notification.dto.message;

import com.kospot.domain.notification.entity.Notification;
import com.kospot.domain.notification.model.NotificationData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationMessage {

    private Long notificationId;
    private String type;
    private String title;
    private String content;
    private String payloadJson;
    private Long sourceId;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationMessage from(Notification notification) {
        return NotificationMessage.builder()
                .notificationId(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .content(notification.getContent())
                .payloadJson(notification.getPayloadJson())
                .sourceId(notification.getSourceId())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedDate())
                .build();
    }

    public static NotificationMessage from(NotificationData notification) {
        return NotificationMessage.builder()
                .notificationId(notification.notificationId())
                .type(notification.type().name())
                .title(notification.title())
                .content(notification.content())
                .payloadJson(notification.payloadJson())
                .sourceId(notification.sourceId())
                .isRead(notification.isRead())
                .createdAt(notification.createdAt())
                .build();
    }
}
