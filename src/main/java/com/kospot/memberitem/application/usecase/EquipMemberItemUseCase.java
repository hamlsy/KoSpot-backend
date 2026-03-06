package com.kospot.memberitem.application.usecase;

import com.kospot.item.domain.vo.ItemType;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.memberitem.application.adaptor.MemberItemAdaptor;
import com.kospot.memberitem.domain.entity.MemberItem;
import com.kospot.memberitem.application.service.MemberItemService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.service.MemberProfileRedisService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class EquipMemberItemUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberItemAdaptor memberItemAdaptor;
    private final MemberItemService memberItemService;

    private final MemberProfileRedisService memberProfileRedisService;

    public void execute(Long memberId, Long memberItemId){
        Member member = memberAdaptor.queryById(memberId);
        MemberItem memberItem = memberItemAdaptor.queryByIdFetchItemAndImage(memberItemId);
        memberItemService.equipItem(member, memberItem);
        if(memberItem.getItem().getItemType() == ItemType.MARKER) {
            memberProfileRedisService.updateMarkerImageUrl(member.getId(), member.getEquippedMarkerImage().getImageUrl());
        }
    }

}
