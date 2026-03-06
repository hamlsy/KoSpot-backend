package com.kospot.friend.presentation.dto.response;

import java.time.LocalDateTime;

public record IncomingFriendRequestResponse(
        Long requestId,
        Long senderMemberId,
        String senderNickname,
        String senderMarkerImageUrl,
        LocalDateTime requestedAt
) {
}
