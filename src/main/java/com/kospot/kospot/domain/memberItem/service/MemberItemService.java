package com.kospot.kospot.domain.memberItem.service;

import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.memberItem.adaptor.MemberItemAdaptor;
import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.kospot.domain.memberItem.repository.MemberItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberItemService {

    private final MemberItemAdaptor memberItemAdaptor;
    private final MemberItemRepository memberItemRepository;

    public void equipItem(Member member, Long memberItemId){
        MemberItem memberItem = memberItemAdaptor.queryByIdFetchItem(memberItemId);
        ItemType memberItemType = memberItem.getItem().getItemType();

        // unEquip
        // todo refactoring 전체를 꼭 탐색해야하나?
        List<MemberItem> equippedMemberItems = memberItemRepository.findEquippedItemByMemberAndItemType(member, memberItemType);
        equippedMemberItems.forEach(MemberItem::unEquip);

        // equip
        memberItem.equip();
    }


    public void deleteAllByItemId(Long itemId){
        memberItemRepository.deleteAllByItemId(itemId);
    }



}
