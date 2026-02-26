package com.kospot.application.notification;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notification.adaptor.NotificationAdaptor;
import com.kospot.domain.notification.entity.Notification;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyNotificationsUseCase {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 50;

    private final NotificationAdaptor notificationAdaptor;

    public List<NotificationResponse.Item> execute(
            Member member,
            int page,
            Integer size,
            NotificationType type,
            Boolean isRead
    ) {
        int pageSize = normalizeSize(size);
        Pageable pageable = PageRequest.of(page, pageSize, Sort.Direction.DESC, "createdDate");

        Page<Notification> notifications = notificationAdaptor.queryPage(member.getId(), type, isRead, pageable);

        return notifications.getContent().stream()
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
