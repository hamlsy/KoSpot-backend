package com.kospot.application.notification;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notification.adaptor.NotificationAdaptor;
import com.kospot.domain.notification.entity.Notification;
import com.kospot.domain.notification.service.NotificationService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class MarkNotificationReadUseCase {

    private final NotificationAdaptor notificationAdaptor;
    private final NotificationService notificationService;

    public void execute(Member member, Long notificationId) {
        Notification notification = notificationAdaptor.queryByIdAndReceiver(notificationId, member.getId());
        notificationService.markRead(notification);
    }
}
