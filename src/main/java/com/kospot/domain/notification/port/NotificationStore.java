package com.kospot.domain.notification.port;

import com.kospot.domain.notification.model.NotificationCreateCommand;
import com.kospot.domain.notification.model.NotificationData;
import com.kospot.domain.notification.vo.NotificationType;

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
