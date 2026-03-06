package com.kospot.item.application.usecase;

import com.kospot.item.application.service.ItemService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteItemFromShopUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ItemService itemService;

    public void execute(Long memberId, Long id){
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        itemService.deleteItemFromShop(id);
    }

}
