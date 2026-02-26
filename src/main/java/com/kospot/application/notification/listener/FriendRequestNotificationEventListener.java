package com.kospot.application.notification.listener;

import com.kospot.domain.notification.entity.Notification;
import com.kospot.domain.notification.event.FriendRequestCreatedEvent;
import com.kospot.domain.notification.service.NotificationService;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.websocket.domain.notification.service.NotificationPushService;
import com.kospot.presentation.notification.dto.message.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestNotificationEventListener {

    private final NotificationService notificationService;
    private final NotificationPushService notificationPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FriendRequestCreatedEvent event) {
        try {
            String payloadJson = String.format(
                    "{\"friendRequestId\":%d,\"senderMemberId\":%d}",
                    event.getFriendRequestId(),
                    event.getSenderMemberId()
            );

            Notification notification = notificationService.create(
                    event.getReceiverMemberId(),
                    NotificationType.FRIEND_REQUEST,
                    "친구 요청",
                    null,
                    payloadJson,
                    event.getFriendRequestId()
            );

            notificationPushService.sendToMember(
                    event.getReceiverMemberId(),
                    NotificationMessage.from(notification)
            );

            log.info("Friend request notification created - FriendRequestId: {}, ReceiverMemberId: {}",
                    event.getFriendRequestId(), event.getReceiverMemberId());

        } catch (Exception e) {
            log.error("Failed to handle FriendRequestCreatedEvent - FriendRequestId: {}", event.getFriendRequestId(), e);
        }
    }
}
