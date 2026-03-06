package com.kospot.domain.friend.adaptor;

import com.kospot.domain.friend.entity.FriendChatRoom;
import com.kospot.domain.friend.entity.FriendRequest;
import com.kospot.domain.friend.entity.Friendship;
import com.kospot.domain.friend.exception.FriendErrorStatus;
import com.kospot.domain.friend.exception.FriendHandler;
import com.kospot.domain.friend.repository.FriendChatRoomRepository;
import com.kospot.domain.friend.repository.FriendRequestRepository;
import com.kospot.domain.friend.repository.FriendSummaryQueryModel;
import com.kospot.domain.friend.repository.FriendshipRepository;
import com.kospot.domain.friend.vo.FriendRequestStatus;
import com.kospot.domain.friend.vo.FriendshipStatus;
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
