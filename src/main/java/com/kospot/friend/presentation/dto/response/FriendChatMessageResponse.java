package com.kospot.friend.presentation.dto.response;

import java.time.LocalDateTime;

public record FriendChatMessageResponse(
        String messageId,
        Long senderMemberId,
        String content,
        LocalDateTime createdAt
) {
}
