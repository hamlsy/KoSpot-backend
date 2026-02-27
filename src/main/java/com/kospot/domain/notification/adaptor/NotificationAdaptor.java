package com.kospot.domain.notification.adaptor;

import com.kospot.domain.notification.model.NotificationData;
import com.kospot.domain.notification.port.NotificationStore;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.exception.object.domain.NotificationHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adaptor
@Transactional(readOnly = true)
public class NotificationAdaptor {

    private final NotificationStore notificationStore;

    public NotificationAdaptor(NotificationStore notificationStore) {
        this.notificationStore = notificationStore;
    }

    public NotificationData queryByIdAndReceiver(Long notificationId, Long receiverMemberId) {
        NotificationData data = notificationStore.getByIdAndReceiver(notificationId, receiverMemberId);
        if (data == null) {
            throw new NotificationHandler(ErrorStatus.NOTIFICATION_NOT_FOUND);
        }
        return data;
    }

    public List<NotificationData> queryPage(Long receiverMemberId, int page, int size, NotificationType type, Boolean isRead) {
        return notificationStore.findPage(receiverMemberId, page, size, type, isRead);
    }

    public long countUnread(Long receiverMemberId) {
        return notificationStore.countUnread(receiverMemberId);
    }
}
