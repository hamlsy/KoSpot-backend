package com.kospot.notification.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.notification.application.adaptor.NotificationAdaptor;
import com.kospot.notification.infrastructure.persistence.NotificationStore;
import com.kospot.common.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class MarkNotificationReadUseCase {

    private final MemberAdaptor memberAdaptor;
    private final NotificationAdaptor notificationAdaptor;
    private final NotificationStore notificationStore;

    public void execute(Long memberId, Long notificationId) {
        Member member = memberAdaptor.queryById(memberId);
        // 존재/권한 검증
        notificationAdaptor.queryByIdAndReceiver(notificationId, member.getId());
        notificationStore.markRead(notificationId, member.getId());
    }
}
