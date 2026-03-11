package com.kospot.friend.infrastructure.persistence;

import com.kospot.friend.domain.entity.FriendChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendChatRoomRepository extends JpaRepository<FriendChatRoom, Long> {

    Optional<FriendChatRoom> findByCanonicalPairKey(String canonicalPairKey);
}
