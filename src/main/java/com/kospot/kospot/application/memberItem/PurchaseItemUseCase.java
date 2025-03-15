package com.kospot.kospot.application.memberItem;

import com.kospot.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.service.ItemService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.memberItem.service.MemberItemService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class PurchaseItemUseCase {

    private final ItemAdaptor itemAdaptor;
    private final ItemService itemService;
    private final MemberItemService memberItemService;
    
    //todo 동시성 해결
    public void execute(Member member, Long itemId) {
        Item item = itemAdaptor.queryById(itemId);
        itemService.purchaseItem(item);
        memberItemService.purchaseItem(member, item);
    }

}
