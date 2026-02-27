package com.kospot.domain.friend.service;

import org.springframework.stereotype.Service;

@Service
public class FriendPairService {

    public String canonicalPairKey(Long memberId1, Long memberId2) {
        long low = Math.min(memberId1, memberId2);
        long high = Math.max(memberId1, memberId2);
        return low + ":" + high;
    }

    public Long lowMemberId(Long memberId1, Long memberId2) {
        return Math.min(memberId1, memberId2);
    }

    public Long highMemberId(Long memberId1, Long memberId2) {
        return Math.max(memberId1, memberId2);
    }

    public Long otherMemberId(Long myMemberId, Long memberLowId, Long memberHighId) {
        return myMemberId.equals(memberLowId) ? memberHighId : memberLowId;
    }
}
