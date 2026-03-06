package com.kospot.application.item;

import com.kospot.domain.item.service.ItemService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.memberitem.service.MemberItemService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional
public class DeleteItemUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ItemService itemService;
    private final MemberItemService memberItemService;

    public void execute(Long memberId, Long id){
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        memberItemService.deleteAllByItemId(id);
        itemService.deleteItemById(id);
    }

}
