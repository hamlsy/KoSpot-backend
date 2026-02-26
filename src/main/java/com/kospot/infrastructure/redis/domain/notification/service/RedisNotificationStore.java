package com.kospot.infrastructure.redis.domain.notification.service;

import com.kospot.domain.notification.model.NotificationCreateCommand;
import com.kospot.domain.notification.model.NotificationData;
import com.kospot.domain.notification.port.NotificationStore;
import com.kospot.domain.notification.vo.NotificationType;
import com.kospot.infrastructure.redis.domain.notification.dao.NotificationRedisRepository;
import com.kospot.infrastructure.redis.domain.notification.vo.NotificationRedisData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class RedisNotificationStore implements NotificationStore {

    private static final int MAX_SCAN = 500;

    private final NotificationRedisRepository notificationRedisRepository;
    private final Duration ttl;

    public RedisNotificationStore(
            NotificationRedisRepository notificationRedisRepository,
            @Value("${notification.ttl-days:14}") long ttlDays
    ) {
        this.notificationRedisRepository = notificationRedisRepository;
        this.ttl = Duration.ofDays(ttlDays);
    }

    @Override
    public NotificationData save(NotificationCreateCommand command) {
        long id = notificationRedisRepository.nextId();
        long nowMillis = System.currentTimeMillis();

        NotificationRedisData data = new NotificationRedisData(
                id,
                command.receiverMemberId(),
                command.type().name(),
                command.title(),
                command.content(),
                command.payloadJson(),
                command.sourceId(),
                false,
                null,
                nowMillis
        );

        notificationRedisRepository.saveItem(data, ttl);
        notificationRedisRepository.addToIndex(command.receiverMemberId(), id, nowMillis, ttl);
        notificationRedisRepository.addUnread(command.receiverMemberId(), id, ttl);

        return toDomain(data);
    }

    @Override
    public void saveAll(List<NotificationCreateCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }

        // 단순 반복 저장 (필요 시 pipeline/batch로 최적화)
        for (NotificationCreateCommand command : commands) {
            save(command);
        }
    }

    @Override
    public NotificationData getByIdAndReceiver(Long notificationId, Long receiverMemberId) {
        NotificationRedisData data = notificationRedisRepository.findItem(notificationId)
                .orElse(null);
        if (data == null) {
            return null;
        }
        if (!Objects.equals(receiverMemberId, data.getReceiverMemberId())) {
            return null;
        }
        return toDomain(data);
    }

    @Override
    public List<NotificationData> findPage(Long receiverMemberId, int page, int size, NotificationType type, Boolean isRead) {
        int effectiveSize = Math.max(1, size);
        long start = (long) page * effectiveSize;

        int collected = 0;
        long cursor = start;
        int scanned = 0;

        List<NotificationData> results = new ArrayList<>(effectiveSize);
        Set<String> orphanIds = new HashSet<>();

        while (collected < effectiveSize && scanned < MAX_SCAN) {
            long end = cursor + (effectiveSize - 1);
            Set<String> ids = notificationRedisRepository.getIndexedIds(receiverMemberId, cursor, end);
            if (ids == null || ids.isEmpty()) {
                break;
            }

            for (String idStr : ids) {
                scanned++;
                Long id = parseLongOrNull(idStr);
                if (id == null) {
                    orphanIds.add(idStr);
                    continue;
                }

                NotificationRedisData data = notificationRedisRepository.findItem(id)
                        .orElse(null);
                if (data == null) {
                    orphanIds.add(idStr);
                    continue;
                }

                // receiver 보호
                if (!Objects.equals(receiverMemberId, data.getReceiverMemberId())) {
                    continue;
                }

                // 필터
                if (type != null && !type.name().equals(data.getType())) {
                    continue;
                }
                if (isRead != null && isRead.booleanValue() != data.isRead()) {
                    continue;
                }

                results.add(toDomain(data));
                collected++;
                if (collected >= effectiveSize) {
                    break;
                }
            }

            cursor = end + 1;
        }

        if (!orphanIds.isEmpty()) {
            notificationRedisRepository.removeFromIndex(receiverMemberId, orphanIds);
            notificationRedisRepository.removeUnread(receiverMemberId, orphanIds);
        }

        return results;
    }

    @Override
    public long countUnread(Long receiverMemberId) {
        // lazy cleanup: 실제 item이 없는 ID는 이후 목록 조회에서 정리됨
        return notificationRedisRepository.countUnread(receiverMemberId);
    }

    @Override
    public void markRead(Long notificationId, Long receiverMemberId) {
        NotificationRedisData data = notificationRedisRepository.findItem(notificationId)
                .orElse(null);
        if (data == null) {
            return;
        }
        if (!Objects.equals(receiverMemberId, data.getReceiverMemberId())) {
            return;
        }

        if (data.isRead()) {
            return;
        }

        long nowMillis = System.currentTimeMillis();

        data.setRead(true);
        data.setReadAtMillis(nowMillis);

        // createdAt 기반 TTL 유지
        Duration remaining = remainingTtl(data.getCreatedAtMillis(), nowMillis);
        if (remaining.isNegative() || remaining.isZero()) {
            notificationRedisRepository.deleteItem(notificationId);
            notificationRedisRepository.removeFromIndex(receiverMemberId, List.of(String.valueOf(notificationId)));
            notificationRedisRepository.removeUnread(receiverMemberId, List.of(String.valueOf(notificationId)));
            return;
        }

        notificationRedisRepository.saveItem(data, remaining);
        notificationRedisRepository.removeUnread(receiverMemberId, List.of(String.valueOf(notificationId)));
    }

    @Override
    public int markAllRead(Long receiverMemberId) {
        Set<String> unreadIds = notificationRedisRepository.getUnreadIds(receiverMemberId);
        if (unreadIds == null || unreadIds.isEmpty()) {
            return 0;
        }

        int updated = 0;
        long nowMillis = System.currentTimeMillis();
        Set<String> orphanIds = new HashSet<>();

        for (String idStr : unreadIds) {
            Long id = parseLongOrNull(idStr);
            if (id == null) {
                orphanIds.add(idStr);
                continue;
            }
            NotificationRedisData data = notificationRedisRepository.findItem(id).orElse(null);
            if (data == null) {
                orphanIds.add(idStr);
                continue;
            }
            if (!Objects.equals(receiverMemberId, data.getReceiverMemberId())) {
                continue;
            }
            if (data.isRead()) {
                orphanIds.add(idStr);
                continue;
            }

            data.setRead(true);
            data.setReadAtMillis(nowMillis);

            Duration remaining = remainingTtl(data.getCreatedAtMillis(), nowMillis);
            if (remaining.isNegative() || remaining.isZero()) {
                notificationRedisRepository.deleteItem(id);
                orphanIds.add(idStr);
                continue;
            }
            notificationRedisRepository.saveItem(data, remaining);
            updated++;
        }

        // unread set 제거 + 고아 정리
        notificationRedisRepository.clearUnread(receiverMemberId);
        if (!orphanIds.isEmpty()) {
            notificationRedisRepository.removeFromIndex(receiverMemberId, orphanIds);
        }
        return updated;
    }

    private Duration remainingTtl(long createdAtMillis, long nowMillis) {
        long expireAt = createdAtMillis + ttl.toMillis();
        long remainingMillis = expireAt - nowMillis;
        return Duration.ofMillis(Math.max(0L, remainingMillis));
    }

    private NotificationData toDomain(NotificationRedisData data) {
        LocalDateTime createdAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(data.getCreatedAtMillis()),
                ZoneId.systemDefault()
        );
        LocalDateTime readAt = null;
        if (data.getReadAtMillis() != null) {
            readAt = LocalDateTime.ofInstant(Instant.ofEpochMilli(data.getReadAtMillis()), ZoneId.systemDefault());
        }

        return new NotificationData(
                data.getNotificationId(),
                data.getReceiverMemberId(),
                NotificationType.valueOf(data.getType()),
                data.getTitle(),
                data.getContent(),
                data.getPayloadJson(),
                data.getSourceId(),
                data.isRead(),
                readAt,
                createdAt
        );
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
