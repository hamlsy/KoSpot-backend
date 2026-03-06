package com.kospot.notification.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.notification.application.adaptor.NotificationAdaptor;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.notification.presentation.dto.response.NotificationResponse;
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
