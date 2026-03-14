package com.kospot.memberitem.application.usecase;

import com.kospot.item.application.adaptor.ItemAdaptor;
import com.kospot.item.domain.entity.Item;
import com.kospot.item.application.service.ItemService;
import com.kospot.common.exception.object.domain.ItemHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.memberitem.application.adaptor.MemberItemAdaptor;
import com.kospot.memberitem.application.service.MemberItemService;
import com.kospot.point.domain.vo.PointHistoryType;
import com.kospot.point.application.service.PointHistoryService;
import com.kospot.point.application.service.PointService;
import com.kospot.common.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class PurchaseItemUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ItemAdaptor itemAdaptor;
    private final MemberItemAdaptor memberItemAdaptor;
    private final ItemService itemService;
    private final MemberItemService memberItemService;
    private final PointService pointService;
    private final PointHistoryService pointHistoryService;

    //todo 동시성 해결 및 트랜잭션 분리
    public void execute(Long memberId, Long itemId) {
        Member member = memberAdaptor.queryById(memberId);
        Item item = itemAdaptor.queryById(itemId);

        if (memberItemAdaptor.existsByMemberAndItem(member, item)) {
            throw new ItemHandler(ErrorStatus.ITEM_ALREADY_OWNED);
        }

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
