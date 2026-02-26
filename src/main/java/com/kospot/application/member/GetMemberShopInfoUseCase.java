package com.kospot.application.member;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberitem.adaptor.MemberItemAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.member.dto.response.MemberShopInfoResponse;
import com.kospot.presentation.memberitem.dto.response.MemberItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberShopInfoUseCase {

    private final MemberItemAdaptor memberItemAdaptor;

    public MemberShopInfoResponse execute(Member member) {
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
