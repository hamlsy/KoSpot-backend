package com.kospot.application.notification;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.notification.adaptor.NotificationAdaptor;
import com.kospot.domain.notification.model.NotificationData;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyNotificationsUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final MemberAdaptor memberAdaptor;
    private final NotificationAdaptor notificationAdaptor;

    public List<NotificationResponse.Item> execute(
            Long memberId,
            int page,
            Integer size,
            NotificationType type,
            Boolean isRead
    ) {
        Member member = memberAdaptor.queryById(memberId);
        int pageSize = normalizeSize(size);
        List<NotificationData> notifications = notificationAdaptor.queryPage(member.getId(), page, pageSize, type, isRead);

        return notifications.stream()
                .map(NotificationResponse.Item::from)
                .toList();
    }

    private int normalizeSize(Integer size) {
        if (size == null) {
            return DEFAULT_SIZE;
        }
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
