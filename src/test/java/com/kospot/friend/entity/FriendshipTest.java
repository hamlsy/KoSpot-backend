package com.kospot.friend.entity;

import com.kospot.friend.domain.entity.Friendship;
import com.kospot.friend.domain.vo.FriendshipStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendshipTest {

    @Test
    @DisplayName("친구 삭제는 상태를 DELETED로 변경한다")
    void deleteChangesStatus() {
        Friendship friendship = Friendship.create(10L, 20L, "10:20");

        friendship.delete();

        assertEquals(FriendshipStatus.DELETED, friendship.getStatus());
        assertNotNull(friendship.getDeletedAt());
    }

    @Test
    @DisplayName("친구 삭제를 여러 번 호출해도 상태는 안정적으로 유지된다")
    void deleteIsIdempotent() {
        Friendship friendship = Friendship.create(10L, 20L, "10:20");

        friendship.delete();
        friendship.delete();

        assertEquals(FriendshipStatus.DELETED, friendship.getStatus());
    }

    @Test
    @DisplayName("참여자 검증이 양쪽 멤버를 모두 허용한다")
    void participantValidationWorksForBothMembers() {
        Friendship friendship = Friendship.create(10L, 20L, "10:20");

        assertTrue(friendship.isParticipant(10L));
        assertTrue(friendship.isParticipant(20L));
    }
}
