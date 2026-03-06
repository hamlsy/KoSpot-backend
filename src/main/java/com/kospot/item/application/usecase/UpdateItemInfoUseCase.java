package com.kospot.item.application.usecase;

import com.kospot.item.application.service.ItemService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.item.presentation.dto.request.ItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateItemInfoUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ItemService itemService;

    public void execute(Long memberId, ItemRequest.UpdateInfo request){
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        itemService.updateItemInfo(request);
    }

}
