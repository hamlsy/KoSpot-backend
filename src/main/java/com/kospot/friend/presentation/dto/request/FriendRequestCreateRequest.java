package com.kospot.friend.presentation.dto.request;

import jakarta.validation.constraints.NotNull;

public record FriendRequestCreateRequest(
        @NotNull Long receiverMemberId
) {
}
