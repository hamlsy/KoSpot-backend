package com.kospot.friend.service;

import com.kospot.domain.friend.service.FriendPairService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FriendPairServiceTest {

    private final FriendPairService friendPairService = new FriendPairService();

    @Test
    @DisplayName("친구 페어 키는 순서와 상관없이 동일하다")
    void canonicalPairKeyIsOrderIndependent() {
        String key1 = friendPairService.canonicalPairKey(10L, 20L);
        String key2 = friendPairService.canonicalPairKey(20L, 10L);

        assertEquals("10:20", key1);
        assertEquals(key1, key2);
    }

    @Test
    @DisplayName("내 아이디 기준 상대 아이디를 올바르게 찾는다")
    void resolvesOtherMemberId() {
        Long otherFromLow = friendPairService.otherMemberId(10L, 10L, 20L);
        Long otherFromHigh = friendPairService.otherMemberId(20L, 10L, 20L);

        assertEquals(20L, otherFromLow);
        assertEquals(10L, otherFromHigh);
    }
}
