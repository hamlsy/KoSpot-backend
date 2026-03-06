package com.kospot.notification.application.listener;

import com.kospot.notification.domain.event.FriendRequestCreatedEvent;
import com.kospot.notification.domain.model.NotificationCreateCommand;
import com.kospot.notification.domain.model.NotificationData;
import com.kospot.notification.infrastructure.persistence.NotificationStore;
import com.kospot.notification.domain.vo.NotificationType;
import com.kospot.notification.infrastructure.websocket.service.NotificationPushService;
import com.kospot.notification.presentation.dto.message.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class FriendRequestNotificationEventListener {

    private final NotificationStore notificationStore;
    private final NotificationPushService notificationPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FriendRequestCreatedEvent event) {
        try {
            String payloadJson = String.format(
                    "{\"friendRequestId\":%d,\"senderMemberId\":%d}",
                    event.getFriendRequestId(),
                    event.getSenderMemberId()
            );

            NotificationData notification = notificationStore.save(new NotificationCreateCommand(
                    event.getReceiverMemberId(),
                    NotificationType.FRIEND_REQUEST,
                    "친구 요청",
                    null,
                    payloadJson,
                    event.getFriendRequestId()
            ));

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
