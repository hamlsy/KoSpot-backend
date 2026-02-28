package com.kospot.domain.friend.service;

import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.domain.friend.entity.Friendship;
import com.kospot.domain.friend.repository.FriendRequestRepository;
import com.kospot.domain.friend.repository.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;

    public FriendRequest saveRequest(FriendRequest request) {
        return friendRequestRepository.save(request);
    }

    public Friendship saveFriendship(Friendship friendship) {
        return friendshipRepository.save(friendship);
    }
}
