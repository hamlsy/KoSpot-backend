package com.kospot.application.notification;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notification.service.NotificationService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class MarkAllNotificationsReadUseCase {

    private final NotificationService notificationService;

    public NotificationResponse.MarkAllReadResult execute(Member member) {
        int updated = notificationService.markAllRead(member.getId());
        return NotificationResponse.MarkAllReadResult.of(updated);
    }
}
