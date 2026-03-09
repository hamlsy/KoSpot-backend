package com.kospot.notification.infrastructure.websocket.service;

import com.kospot.doc.infrastructure.annotation.WebSocketDoc;
import com.kospot.notification.infrastructure.websocket.constants.NotificationChannelConstants;
import com.kospot.notification.presentation.dto.message.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPushService {


    private final SimpUserRegistry simpUserRegistry;
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
        SimpUser user = simpUserRegistry.getUser(String.valueOf(memberId));
        log.info("before send - username={}, userExists={}", memberId, user != null);
        Set<SimpUser> users = simpUserRegistry.getUsers();
        if (user != null) {
            for (SimpSession session : user.getSessions()) {
                log.info(" sessionId={}", session.getId());
                for (SimpSubscription sub : session.getSubscriptions()) {
                    log.info("  subId={}, dest={}", sub.getId(), sub.getDestination());
                }
            }
        }
        simpMessagingTemplate.convertAndSendToUser(
                String.valueOf(memberId),
                NotificationChannelConstants.getPersonalNotificationSendDestination(),
                message
        );
    }
}
