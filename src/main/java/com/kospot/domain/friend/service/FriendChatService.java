package com.kospot.domain.friend.service;

import com.kospot.domain.friend.entity.FriendChatMessage;
import com.kospot.domain.friend.entity.FriendChatRoom;
import com.kospot.domain.friend.repository.FriendChatMessageRepository;
import com.kospot.domain.friend.repository.FriendChatRoomRepository;
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
