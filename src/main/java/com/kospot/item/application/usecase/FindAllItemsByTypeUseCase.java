package com.kospot.item.application.usecase;

import com.kospot.item.application.adaptor.ItemAdaptor;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.item.presentation.dto.response.ItemResponse;
import com.kospot.item.domain.vo.ItemType;
import com.kospot.common.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;

@UseCase
@RequiredArgsConstructor
public class FindAllItemsByTypeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ItemAdaptor itemAdaptor;

    public List<ItemResponse> execute(Long memberId, String itemTypeKey) {
        Member member = memberAdaptor.queryById(memberId);
        ItemType itemType = ItemType.fromKey(itemTypeKey);
        return itemAdaptor.queryAvailableItemsByWithOwnersByFetchImage(member, itemType);
    }

}
