package com.kospot.domain.notification.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.notification.vo.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notification_receiver_created", columnList = "receiver_member_id,created_date"),
                @Index(name = "idx_notification_receiver_unread_created", columnList = "receiver_member_id,is_read,created_date")
        }
)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @Column(name = "receiver_member_id", nullable = false)
    private Long receiverMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private NotificationType type;

    @Column(name = "title", length = 200)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Lob
    @Column(name = "payload_json", columnDefinition = "LONGTEXT")
    private String payloadJson;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public static Notification create(
            Long receiverMemberId,
            NotificationType type,
            String title,
            String content,
            String payloadJson,
            Long sourceId
    ) {
        Notification notification = new Notification();
        notification.receiverMemberId = receiverMemberId;
        notification.type = type;
        notification.title = title;
        notification.content = content;
        notification.payloadJson = payloadJson;
        notification.sourceId = sourceId;
        notification.isRead = false;
        notification.readAt = null;
        return notification;
    }

    public void markRead(LocalDateTime now) {
        if (this.isRead) {
            return;
        }
        this.isRead = true;
        this.readAt = now;
    }
}
