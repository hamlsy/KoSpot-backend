package com.kospot.infrastructure.websocket.domain.friend.constants;

import static com.kospot.infrastructure.websocket.constants.CommonWebSocketChannelConstants.PREFIX_TOPIC;

public final class FriendChatChannelConstants {

    private FriendChatChannelConstants() {
    }

    public static final String PREFIX_FRIEND_CHAT_ROOM = PREFIX_TOPIC + "friend/chat-rooms/";

    public static String getFriendChatRoomChannel(Long roomId) {
        if (roomId == null || roomId <= 0) {
            throw new IllegalArgumentException("roomId must be a positive number");
        }
        return PREFIX_FRIEND_CHAT_ROOM + roomId;
    }

    public static Long extractRoomIdFromDestination(String destination) {
        if (destination == null || !destination.startsWith(PREFIX_FRIEND_CHAT_ROOM)) {
            return null;
        }

        String value = destination.substring(PREFIX_FRIEND_CHAT_ROOM.length());
        int slashIndex = value.indexOf('/');
        String roomId = slashIndex >= 0 ? value.substring(0, slashIndex) : value;

        try {
            return Long.parseLong(roomId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
