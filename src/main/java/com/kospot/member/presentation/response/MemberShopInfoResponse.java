package com.kospot.member.presentation.response;

import com.kospot.presentation.memberitem.dto.response.MemberItemResponse;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberShopInfoResponse {

    private int currentPoint;
    private List<MemberItemResponse> equippedItems;
    private List<MemberItemResponse> ownedItems;
}
