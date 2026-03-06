package com.kospot.friend.application.service;

import com.kospot.friend.domain.entity.FriendChatMessage;
import com.kospot.friend.domain.entity.FriendChatRoom;
import com.kospot.friend.infrastructure.persistence.FriendChatMessageRepository;
import com.kospot.friend.infrastructure.persistence.FriendChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendChatService {

    private final FriendChatRoomRepository friendChatRoomRepository;
    private final FriendChatMessageRepository friendChatMessageRepository;

    public FriendChatRoom saveRoom(FriendChatRoom room) {
        return friendChatRoomRepository.save(room);
    }

    public List<FriendChatMessage> queryRecentMessages(Long roomId, int page, int size) {
        return friendChatMessageRepository.findRecentMessagesByRoomId(roomId, PageRequest.of(page, size));
    }
}
