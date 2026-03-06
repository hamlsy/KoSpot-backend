package com.kospot.application.memberitem;

import com.kospot.item.domain.vo.ItemType;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.memberitem.adaptor.MemberItemAdaptor;
import com.kospot.domain.memberitem.entity.MemberItem;
import com.kospot.domain.memberitem.service.MemberItemService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.member.service.MemberProfileRedisService;
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
