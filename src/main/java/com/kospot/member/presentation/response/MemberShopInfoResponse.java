package com.kospot.member.presentation.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberShopInfoResponse {

    private int currentPoint;
    private List<Long> equippedItemIds;
    private List<Long> ownedMemberItemIds;
}
