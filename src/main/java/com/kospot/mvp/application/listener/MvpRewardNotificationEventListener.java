package com.kospot.mvp.application.listener;

import com.kospot.mvp.domain.event.MvpRewardGrantedEvent;
import com.kospot.domain.notification.model.NotificationCreateCommand;
import com.kospot.domain.notification.model.NotificationData;
import com.kospot.domain.notification.port.NotificationStore;
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
public class MvpRewardNotificationEventListener {

    private final NotificationStore notificationStore;
    private final NotificationPushService notificationPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MvpRewardGrantedEvent event) {
        try {
            String payloadJson = String.format(
                    "{\"mvpDate\":\"%s\",\"rewardPoint\":%d,\"roadViewGameId\":%d}",
                    event.getMvpDate(),
                    event.getRewardPoint(),
                    event.getRoadViewGameId()
            );

            NotificationData notification = notificationStore.save(new NotificationCreateCommand(
                    event.getMemberId(),
                    NotificationType.MVP_REWARD,
                    "오늘의 MVP 보상",
                    event.getRewardPoint() + "포인트가 지급되었습니다.",
                    payloadJson,
                    event.getRoadViewGameId()
            ));

            notificationPushService.sendToMember(
                    event.getMemberId(),
                    NotificationMessage.from(notification)
            );

        } catch (Exception e) {
            log.error("Failed to send MVP reward notification. memberId={}", event.getMemberId(), e);
        }
    }
}
