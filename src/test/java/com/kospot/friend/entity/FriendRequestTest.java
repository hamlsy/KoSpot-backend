package com.kospot.friend.entity;

import com.kospot.friend.domain.entity.FriendRequest;
import com.kospot.friend.domain.exception.FriendHandler;
import com.kospot.friend.domain.vo.FriendRequestStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FriendRequestTest {

    @Test
    @DisplayName("요청 수신자가 승인하면 상태가 APPROVED로 변경된다")
    void approveByReceiverChangesStatus() {
        FriendRequest request = FriendRequest.create(1L, 2L, "1:2", LocalDateTime.now().plusDays(7));

        request.approve(2L);

        assertEquals(FriendRequestStatus.APPROVED, request.getStatus());
    }

    @Test
    @DisplayName("요청 수신자가 아니면 승인할 수 없다")
    void approveByNonReceiverThrows() {
        FriendRequest request = FriendRequest.create(1L, 2L, "1:2", LocalDateTime.now().plusDays(7));

        assertThrows(FriendHandler.class, () -> request.approve(3L));
    }

    @Test
    @DisplayName("이미 승인된 요청은 다시 거절할 수 없다")
    void rejectAfterApprovedThrows() {
        FriendRequest request = FriendRequest.create(1L, 2L, "1:2", LocalDateTime.now().plusDays(7));
        request.approve(2L);

        assertThrows(FriendHandler.class, () -> request.reject(2L));
    }
}
