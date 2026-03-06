package com.kospot.infrastructure.websocket.domain.notification.service;

import com.kospot.common.doc.annotation.WebSocketDoc;
import com.kospot.infrastructure.websocket.domain.notification.constants.NotificationChannelConstants;
import com.kospot.presentation.notification.dto.message.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {

    private final SimpMessagingTemplate simpMessagingTemplate;

    @WebSocketDoc(
            payloadType = NotificationMessage.class,
            trigger = "알림 발생(공지사항/시스템)",
            description = "전역 알림 채널로 알림을 브로드캐스트합니다.",
            destination = NotificationChannelConstants.GLOBAL_NOTIFICATION_CHANNEL
    )
    public void sendGlobal(NotificationMessage message) {
        simpMessagingTemplate.convertAndSend(NotificationChannelConstants.GLOBAL_NOTIFICATION_CHANNEL, message);
    }

    @WebSocketDoc(
            payloadType = NotificationMessage.class,
            trigger = "알림 발생(개인)",
            description = "특정 사용자에게 개인 알림을 전송합니다.",
            destination = "/user/queue/notification"
    )
    public void sendToMember(Long memberId, NotificationMessage message) {
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(memberId),
                NotificationChannelConstants.getPersonalNotificationSendDestination(),
                message
        );
    }
}
