package com.kospot.application.item;

import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.item.dto.request.ItemRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class UpdateItemInfoUseCase {

    private final ItemService itemService;

    public void execute(Member member, ItemRequest.UpdateInfo request){
        member.validateAdmin();
        itemService.updateItemInfo(request);
    }

}
