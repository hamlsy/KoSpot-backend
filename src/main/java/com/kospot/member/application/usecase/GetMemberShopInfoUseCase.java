package com.kospot.member.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.memberitem.application.adaptor.MemberItemAdaptor;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.presentation.response.MemberShopInfoResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberShopInfoUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberItemAdaptor memberItemAdaptor;

    public MemberShopInfoResponse execute(Long memberId) {
        int currentPoint = memberAdaptor.queryPointById(memberId);
        List<Long> equippedItemIds = memberItemAdaptor.queryEquippedItemIdsByMemberId(memberId);
        List<Long> ownedMemberItemIds = memberItemAdaptor.queryOwnedItemIdsByMemberId(memberId);

        return MemberShopInfoResponse.builder()
                .currentPoint(currentPoint)
                .equippedItemIds(equippedItemIds)
                .ownedMemberItemIds(ownedMemberItemIds)
                .build();
    }
}
