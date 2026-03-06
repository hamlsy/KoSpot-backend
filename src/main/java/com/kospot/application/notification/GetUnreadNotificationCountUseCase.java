package com.kospot.application.notification;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.notification.adaptor.NotificationAdaptor;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.presentation.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetUnreadNotificationCountUseCase {

    private final MemberAdaptor memberAdaptor;
    private final NotificationAdaptor notificationAdaptor;

    public NotificationResponse.UnreadCount execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        long unreadCount = notificationAdaptor.countUnread(member.getId());
        return NotificationResponse.UnreadCount.of(unreadCount);
    }
}
