package com.kospot.kospot.application.item;

import com.kospot.kospot.domain.item.dto.request.ItemRequest;
import com.kospot.kospot.domain.item.service.ItemService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@Transactional
@RequiredArgsConstructor
public class RegisterItemUseCase {

    private final ItemService itemService;

    public void execute(Member member, ItemRequest.Create request){
        itemService.registerItem(member, request);
    }

}
