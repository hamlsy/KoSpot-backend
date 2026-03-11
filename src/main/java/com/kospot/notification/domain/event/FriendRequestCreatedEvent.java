package com.kospot.notification.domain.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FriendRequestCreatedEvent {

    private final Long friendRequestId;
    private final Long senderMemberId;
    private final Long receiverMemberId;
    private final LocalDateTime createdAt;
}
