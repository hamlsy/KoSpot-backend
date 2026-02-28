package com.kospot.domain.friend.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record FriendChatStreamMessage(
        String messageId,
        Long roomId,
        Long senderMemberId,
        String content,
        LocalDateTime createdAt,
        int retryCount
) {
    public static FriendChatStreamMessage create(Long roomId, Long senderMemberId, String content) {
        return new FriendChatStreamMessage(
                UUID.randomUUID().toString(),
                roomId,
                senderMemberId,
                content,
                LocalDateTime.now(),
                0
        );
    }
}
