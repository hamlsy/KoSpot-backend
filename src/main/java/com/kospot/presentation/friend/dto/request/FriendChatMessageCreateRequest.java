package com.kospot.presentation.friend.dto.request;

import jakarta.validation.constraints.NotBlank;

public record FriendChatMessageCreateRequest(
        @NotBlank String content
) {
}
