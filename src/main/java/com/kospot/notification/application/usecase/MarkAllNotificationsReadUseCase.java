package com.kospot.notification.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.notification.infrastructure.persistence.NotificationStore;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.notification.presentation.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class MarkAllNotificationsReadUseCase {

    private final MemberAdaptor memberAdaptor;
    private final NotificationStore notificationStore;

    public NotificationResponse.MarkAllReadResult execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        int updated = notificationStore.markAllRead(member.getId());
        return NotificationResponse.MarkAllReadResult.of(updated);
    }
}
