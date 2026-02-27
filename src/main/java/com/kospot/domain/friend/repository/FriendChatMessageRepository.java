package com.kospot.domain.friend.repository;

import com.kospot.domain.friend.entity.FriendChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendChatMessageRepository extends JpaRepository<FriendChatMessage, Long> {

    @Query("select m from FriendChatMessage m where m.roomId = :roomId order by m.createdDate desc")
    List<FriendChatMessage> findRecentMessagesByRoomId(@Param("roomId") Long roomId, Pageable pageable);
}
