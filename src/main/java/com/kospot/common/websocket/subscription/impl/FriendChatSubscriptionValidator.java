package com.kospot.common.websocket.subscription.impl;

import com.kospot.friend.application.adaptor.FriendAdaptor;
import com.kospot.friend.domain.entity.FriendChatRoom;
import com.kospot.common.websocket.auth.WebSocketMemberPrincipal;
import com.kospot.common.websocket.domain.friend.constants.FriendChatChannelConstants;
import com.kospot.common.websocket.subscription.SubscriptionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FriendChatSubscriptionValidator implements SubscriptionValidator {

    private static final Logger log = LoggerFactory.getLogger(FriendChatSubscriptionValidator.class);

    private final FriendAdaptor friendAdaptor;

    public FriendChatSubscriptionValidator(FriendAdaptor friendAdaptor) {
        this.friendAdaptor = friendAdaptor;
    }

    @Override
    public boolean canSubscribe(WebSocketMemberPrincipal principal, String destination) {
        Long memberId = resolveMemberId(principal);
        if (memberId == null || memberId <= 0) {
            return false;
        }

        Long roomId = FriendChatChannelConstants.extractRoomIdFromDestination(destination);
        if (roomId == null) {
            return false;
        }

        try {
            FriendChatRoom room = friendAdaptor.queryChatRoomById(roomId);
            return room.isParticipant(memberId);
        } catch (Exception e) {
            log.warn("Friend chat subscription validation failed. memberId={}, destination={}",
                    memberId, destination);
            return false;
        }
    }

    @Override
    public boolean supports(String destination) {
        return destination != null && destination.startsWith(FriendChatChannelConstants.PREFIX_FRIEND_CHAT_ROOM);
    }

    @Override
    public int getPriority() {
        return 250;
    }

    private Long resolveMemberId(WebSocketMemberPrincipal principal) {
        if (principal == null || principal.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
