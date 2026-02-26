package com.kospot.presentation.notification.dto.response;

import com.kospot.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

public class NotificationResponse {

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor
    public static class Item {
        private Long notificationId;
        private Long receiverMemberId;
        private String type;
        private String title;
        private String content;
        private String payloadJson;
        private Long sourceId;
        private Boolean isRead;
        private LocalDateTime readAt;
        private LocalDateTime createdAt;

        public static Item from(Notification notification) {
            return Item.builder()
                    .notificationId(notification.getId())
                    .receiverMemberId(notification.getReceiverMemberId())
                    .type(notification.getType().name())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .payloadJson(notification.getPayloadJson())
                    .sourceId(notification.getSourceId())
                    .isRead(notification.isRead())
                    .readAt(notification.getReadAt())
                    .createdAt(notification.getCreatedDate())
                    .build();
        }
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor
    public static class UnreadCount {
        private Long unreadCount;

        public static UnreadCount of(long unreadCount) {
            return UnreadCount.builder()
                    .unreadCount(unreadCount)
                    .build();
        }
    }

    @Getter
    @Builder
    @ToString
    @AllArgsConstructor
    public static class MarkAllReadResult {
        private Integer updatedCount;

        public static MarkAllReadResult of(int updatedCount) {
            return MarkAllReadResult.builder()
                    .updatedCount(updatedCount)
                    .build();
        }
    }
}
