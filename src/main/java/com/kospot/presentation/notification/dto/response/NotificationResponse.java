package com.kospot.presentation.notification.dto.response;

import com.kospot.domain.notification.model.NotificationData;
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

        public static Item from(NotificationData notification) {
            return Item.builder()
                    .notificationId(notification.notificationId())
                    .receiverMemberId(notification.receiverMemberId())
                    .type(notification.type().name())
                    .title(notification.title())
                    .content(notification.content())
                    .payloadJson(notification.payloadJson())
                    .sourceId(notification.sourceId())
                    .isRead(notification.isRead())
                    .readAt(notification.readAt())
                    .createdAt(notification.createdAt())
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
