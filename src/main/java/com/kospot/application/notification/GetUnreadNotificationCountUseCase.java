package com.kospot.application.notification;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notification.adaptor.NotificationAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUnreadNotificationCountUseCase {

    private final NotificationAdaptor notificationAdaptor;

    public NotificationResponse.UnreadCount execute(Member member) {
        long unreadCount = notificationAdaptor.countUnread(member.getId());
        return NotificationResponse.UnreadCount.of(unreadCount);
    }
}
