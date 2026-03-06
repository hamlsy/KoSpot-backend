package com.kospot.application.notification;

import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notification.adaptor.NotificationAdaptor;
import com.kospot.domain.notification.port.NotificationStore;
import com.kospot.infrastructure.annotation.usecase.UseCase;
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
