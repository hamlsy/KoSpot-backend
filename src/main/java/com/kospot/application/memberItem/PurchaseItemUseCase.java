package com.kospot.application.memberItem;

import com.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberItem.service.MemberItemService;
import com.kospot.domain.point.entity.PointHistoryType;
import com.kospot.domain.point.service.PointHistoryService;
import com.kospot.domain.point.service.PointService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
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
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;

    //todo 동시성 해결 및 트랜잭션 분리
    public void execute(Member member, Long itemId) {
        Item item = itemAdaptor.queryById(itemId);
        int itemPrice = item.getPrice();

        // member point 감소
        pointService.usePoint(member, itemPrice);

        // item 재고 감소
        itemService.purchaseItem(item);

        // memberItem 생성
        memberItemService.purchaseItem(member, item);

        // point history 생성
        pointHistoryService.savePointHistory(member, -1 * itemPrice, PointHistoryType.ITEM_PURCHASE);
    }

}
