package com.kospot.domain.notification.service;

import com.kospot.domain.notification.entity.Notification;
import com.kospot.domain.notification.repository.NotificationRepository;
import com.kospot.domain.notification.vo.NotificationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification create(
            Long receiverMemberId,
            NotificationType type,
            String title,
            String content,
            String payloadJson,
            Long sourceId
    ) {
        Notification notification = Notification.create(
                receiverMemberId,
                type,
                title,
                content,
                payloadJson,
                sourceId
        );
        return notificationRepository.save(notification);
    }

    public void markRead(Notification notification) {
        notification.markRead(LocalDateTime.now());
    }

    public int markAllRead(Long receiverMemberId) {
        return notificationRepository.markAllRead(receiverMemberId, LocalDateTime.now());
    }
}
