package com.kospot.friend.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FriendChatMessageCreateRequest(
        @NotBlank String content
) {
}
