package com.kospot.member.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.memberitem.adaptor.MemberItemAdaptor;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.presentation.response.MemberShopInfoResponse;
import com.kospot.presentation.memberitem.dto.response.MemberItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberShopInfoUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberItemAdaptor memberItemAdaptor;

    public MemberShopInfoResponse execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        List<MemberItemResponse> ownedItems = memberItemAdaptor.queryAllByMemberFetch(member);
        List<MemberItemResponse> equippedItems = ownedItems.stream()
                .filter(item -> item.getIsEquipped())
                .collect(Collectors.toList());

        return MemberShopInfoResponse.builder()
                .currentPoint(member.getPoint())
                .equippedItems(equippedItems)
                .ownedItems(ownedItems)
                .build();
    }
}
