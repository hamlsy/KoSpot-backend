package com.kospot.application.item;

import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.item.entity.Item;
import com.kospot.presentation.item.dto.request.ItemRequest;
import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@Transactional
@RequiredArgsConstructor
public class RegisterItemUseCase {

    private final ItemService itemService;
    private final ImageService imageService;

    public void execute(Member member, ItemRequest.Create request){
        member.validateAdmin();

        // insert item
        Item item = itemService.registerItem(request);

        // insert image
        imageService.uploadItemImage(request.getImage(), item);
    }

}
