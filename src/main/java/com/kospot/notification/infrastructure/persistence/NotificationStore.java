package com.kospot.notification.infrastructure.persistence;

import com.kospot.notification.domain.model.NotificationCreateCommand;
import com.kospot.notification.domain.model.NotificationData;
import com.kospot.notification.domain.vo.NotificationType;

import java.util.List;

public interface NotificationStore {

    NotificationData save(NotificationCreateCommand command);

    void saveAll(List<NotificationCreateCommand> commands);

    NotificationData getByIdAndReceiver(Long notificationId, Long receiverMemberId);

    List<NotificationData> findPage(
            Long receiverMemberId,
            int page,
            int size,
            NotificationType type,
            Boolean isRead
    );

    long countUnread(Long receiverMemberId);

    void markRead(Long notificationId, Long receiverMemberId);

    int markAllRead(Long receiverMemberId);
}
