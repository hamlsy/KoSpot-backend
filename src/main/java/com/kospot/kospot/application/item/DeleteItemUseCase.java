package com.kospot.kospot.application.item;

import com.kospot.kospot.domain.item.service.ItemService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.memberItem.service.MemberItemService;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteItemUseCase {

    private ItemService itemService;
    private MemberItemService memberItemService;

    public void execute(Member member, Long id){
        member.validateAdmin();
        memberItemService.deleteAllByItemId(id);
        itemService.deleteItemById(id);
    }

}
