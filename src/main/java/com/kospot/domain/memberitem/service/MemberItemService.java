package com.kospot.domain.memberitem.service;

import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberitem.adaptor.MemberItemAdaptor;
import com.kospot.domain.memberitem.entity.MemberItem;
import com.kospot.domain.memberitem.repository.MemberItemRepository;
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

    public void equipItem(Member member, MemberItem memberItem) {


        ItemType memberItemType = memberItem.getItem().getItemType();
        if(memberItemType.equals(ItemType.MARKER)) {
            member.equippedMarkerImage(memberItem.getItem().getImage());
        }

        // unEquip
        unEquippedItems(member, memberItemType);

        // equip
        memberItem.equip();
    }

    // todo refactoring 전체를 꼭 탐색해야하나?
    private void unEquippedItems(Member member, ItemType itemType){
        List<MemberItem> equippedMemberItems = memberItemRepository.findEquippedItemByMemberAndItemType(member, itemType);
        if(equippedItemsNotEmpty(equippedMemberItems)){
            equippedMemberItems.forEach(MemberItem::unEquip);
        }
    }

    private static boolean equippedItemsNotEmpty(List<MemberItem> equippedMemberItems) {
        return !equippedMemberItems.isEmpty();
    }

    public void deleteAllByItemId(Long itemId) {
        memberItemRepository.deleteAllByItemId(itemId);
    }

    public MemberItem purchaseItem(Member member, Item item) {
        MemberItem memberItem = MemberItem.create(member, item);
        memberItemRepository.save(memberItem);
    }


}
