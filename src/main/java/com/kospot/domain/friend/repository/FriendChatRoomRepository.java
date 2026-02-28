package com.kospot.domain.friend.repository;

import com.kospot.domain.friend.entity.FriendChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FriendChatRoomRepository extends JpaRepository<FriendChatRoom, Long> {

    Optional<FriendChatRoom> findByCanonicalPairKey(String canonicalPairKey);
}
