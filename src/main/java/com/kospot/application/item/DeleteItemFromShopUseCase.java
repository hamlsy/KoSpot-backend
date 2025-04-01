package com.kospot.application.item;

import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.entity.Member;
import com.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteItemFromShopUseCase {

    private final ItemService itemService;

    public void execute(Member member, Long id){
        member.validateAdmin();
        itemService.deleteItemFromShop(id);
    }

}
