package com.kospot.application.item;

import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class RestoreItemToShopUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ItemService itemService;

    public void execute(Long memberId, Long id){
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        itemService.restoreItemToShop(id);
    }

}
