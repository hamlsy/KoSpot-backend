package com.kospot.domain.notification.repository;

import com.kospot.domain.notification.entity.Notification;
import com.kospot.domain.notification.vo.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByReceiverMemberId(Long receiverMemberId, Pageable pageable);

    Page<Notification> findAllByReceiverMemberIdAndType(Long receiverMemberId, NotificationType type, Pageable pageable);

    Page<Notification> findAllByReceiverMemberIdAndIsRead(Long receiverMemberId, boolean isRead, Pageable pageable);

    Page<Notification> findAllByReceiverMemberIdAndTypeAndIsRead(
            Long receiverMemberId,
            NotificationType type,
            boolean isRead,
            Pageable pageable
    );

    Optional<Notification> findByIdAndReceiverMemberId(Long id, Long receiverMemberId);

    long countByReceiverMemberIdAndIsReadFalse(Long receiverMemberId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true, n.readAt = :readAt " +
            "where n.receiverMemberId = :receiverMemberId and n.isRead = false")
    int markAllRead(@Param("receiverMemberId") Long receiverMemberId, @Param("readAt") LocalDateTime readAt);
}
