package com.kospot.domain.notification.adaptor;

import com.kospot.domain.notification.entity.Notification;
import com.kospot.domain.notification.repository.NotificationRepository;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.exception.object.domain.NotificationHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@Transactional(readOnly = true)
public class NotificationAdaptor {

    private final NotificationRepository notificationRepository;

    public NotificationAdaptor(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public Notification queryByIdAndReceiver(Long notificationId, Long receiverMemberId) {
        return notificationRepository.findByIdAndReceiverMemberId(notificationId, receiverMemberId)
                .orElseThrow(() -> new NotificationHandler(ErrorStatus.NOTIFICATION_NOT_FOUND));
    }

    public Page<Notification> queryPageByReceiver(Long receiverMemberId, Pageable pageable) {
        return notificationRepository.findAllByReceiverMemberId(receiverMemberId, pageable);
    }

    public Page<Notification> queryPageByReceiverAndType(Long receiverMemberId, NotificationType type, Pageable pageable) {
        return notificationRepository.findAllByReceiverMemberIdAndType(receiverMemberId, type, pageable);
    }

    public Page<Notification> queryPage(Long receiverMemberId, NotificationType type, Boolean isRead, Pageable pageable) {
        if (type == null && isRead == null) {
            return notificationRepository.findAllByReceiverMemberId(receiverMemberId, pageable);
        }

        if (type != null && isRead == null) {
            return notificationRepository.findAllByReceiverMemberIdAndType(receiverMemberId, type, pageable);
        }

        if (type == null) {
            return notificationRepository.findAllByReceiverMemberIdAndIsRead(receiverMemberId, isRead, pageable);
        }

        return notificationRepository.findAllByReceiverMemberIdAndTypeAndIsRead(receiverMemberId, type, isRead, pageable);
    }

    public long countUnread(Long receiverMemberId) {
        return notificationRepository.countByReceiverMemberIdAndIsReadFalse(receiverMemberId);
    }
}
