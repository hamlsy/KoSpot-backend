package com.kospot.application.item;

import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberItem.service.MemberItemService;
import com.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteItemUseCase {

    private final ItemService itemService;
    private final MemberItemService memberItemService;

    public void execute(Member member, Long id){
        member.validateAdmin();
        memberItemService.deleteAllByItemId(id);
        itemService.deleteItemById(id);
    }

}
