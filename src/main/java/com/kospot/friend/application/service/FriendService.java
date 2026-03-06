package com.kospot.friend.application.service;

import com.kospot.friend.domain.entity.FriendRequest;
import com.kospot.friend.domain.entity.Friendship;
import com.kospot.friend.infrastructure.persistence.FriendRequestRepository;
import com.kospot.friend.infrastructure.persistence.FriendshipRepository;
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
