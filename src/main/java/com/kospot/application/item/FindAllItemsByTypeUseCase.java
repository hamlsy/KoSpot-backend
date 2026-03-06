package com.kospot.application.item;

import com.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.presentation.item.dto.response.ItemResponse;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.infrastructure.annotation.usecase.UseCase;
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
