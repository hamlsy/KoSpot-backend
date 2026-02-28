package com.kospot.presentation.friend.dto.request;

import jakarta.validation.constraints.NotNull;

public record FriendRequestCreateRequest(
        @NotNull Long receiverMemberId
) {
}
