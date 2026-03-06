package com.kospot.friend.application.adaptor;

import com.kospot.friend.domain.entity.FriendChatRoom;
import com.kospot.friend.domain.entity.FriendRequest;
import com.kospot.friend.domain.entity.Friendship;
import com.kospot.friend.domain.exception.FriendErrorStatus;
import com.kospot.friend.domain.exception.FriendHandler;
import com.kospot.friend.infrastructure.persistence.FriendChatRoomRepository;
import com.kospot.friend.infrastructure.persistence.FriendRequestRepository;
import com.kospot.friend.infrastructure.persistence.FriendSummaryQueryModel;
import com.kospot.friend.infrastructure.persistence.FriendshipRepository;
import com.kospot.friend.domain.vo.FriendRequestStatus;
import com.kospot.friend.domain.vo.FriendshipStatus;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.common.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FriendAdaptor {

    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendChatRoomRepository friendChatRoomRepository;

    public Optional<FriendRequest> queryRequestByCanonicalPair(String canonicalPairKey) {
        return friendRequestRepository.findByCanonicalPairKey(canonicalPairKey);
    }

    public List<FriendRequest> queryRequestsByCanonicalPairs(List<String> canonicalPairKeys) {
        return friendRequestRepository.findByCanonicalPairKeyIn(canonicalPairKeys);
    }

    public FriendRequest queryRequestById(Long requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new FriendHandler(FriendErrorStatus.FRIEND_REQUEST_NOT_FOUND));
    }

    public List<FriendRequest> queryIncomingPendingRequests(Long receiverMemberId, int page, int size) {
        return friendRequestRepository.findIncomingByReceiverAndStatus(
                receiverMemberId,
                FriendRequestStatus.PENDING,
                PageRequest.of(page, size));
    }

    public Optional<Friendship> queryFriendshipByCanonicalPair(String canonicalPairKey) {
        return friendshipRepository.findByCanonicalPairKey(canonicalPairKey);
    }

    public List<Friendship> queryFriendshipsByCanonicalPairs(List<String> canonicalPairKeys) {
        return friendshipRepository.findByCanonicalPairKeyIn(canonicalPairKeys);
    }

    public List<FriendSummaryQueryModel> queryFriendSummaries(Long memberId) {
        return friendshipRepository.findFriendSummaries(memberId, FriendshipStatus.ACTIVE, GameMode.ROADVIEW);
    }

    public FriendChatRoom queryChatRoomById(Long roomId) {
        return friendChatRoomRepository.findById(roomId)
                .orElseThrow(() -> new FriendHandler(FriendErrorStatus.FRIEND_CHAT_ROOM_NOT_FOUND));
    }

    public Optional<FriendChatRoom> queryChatRoomByCanonicalPair(String canonicalPairKey) {
        return friendChatRoomRepository.findByCanonicalPairKey(canonicalPairKey);
    }
}
