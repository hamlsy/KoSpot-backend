package com.kospot.application.notification.listener;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.notice.application.event.NoticeCreatedEvent;
import com.kospot.domain.notification.model.NotificationCreateCommand;
import com.kospot.domain.notification.port.NotificationStore;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.websocket.domain.notification.service.NotificationPushService;
import com.kospot.presentation.notification.dto.message.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeNotificationEventListener {

    private final MemberAdaptor memberAdaptor;
    private final NotificationStore notificationStore;
    private final NotificationPushService notificationPushService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(NoticeCreatedEvent event) {
        try {
            // 1) Redis 알림 저장 (전체 사용자 fan-out, TTL=2주)
            List<Member> members = memberAdaptor.findAll();

            String payloadJson = String.format("{\"noticeId\":%d}", event.getNoticeId());
            List<NotificationCreateCommand> commands = members.stream()
                    .map(member -> new NotificationCreateCommand(
                            member.getId(),
                            NotificationType.NOTICE,
                            event.getTitle(),
                            null,
                            payloadJson,
                            event.getNoticeId()
                    ))
                    .toList();

            notificationStore.saveAll(commands);

            // 2) 전역 STOMP 푸시
            NotificationMessage message = NotificationMessage.builder()
                    .notificationId(null)
                    .type(NotificationType.NOTICE.name())
                    .title(event.getTitle())
                    .content(null)
                    .payloadJson(payloadJson)
                    .sourceId(event.getNoticeId())
                    .isRead(false)
                    .createdAt(event.getCreatedAt())
                    .build();

            notificationPushService.sendGlobal(message);

            log.info("Notice notification published - NoticeId: {}, MemberCount: {}", event.getNoticeId(), members.size());

        } catch (Exception e) {
            // 알림 처리 실패가 공지 생성 자체를 망치지 않도록 커밋 이후 단계에서 격리
            log.error("Failed to handle NoticeCreatedEvent - NoticeId: {}", event.getNoticeId(), e);
        }
    }
}
