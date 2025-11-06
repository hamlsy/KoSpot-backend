package com.kospot.application.item;

import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.presentation.item.dto.request.ItemRequest;
import com.kospot.domain.item.service.ItemService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@UseCase
@Transactional
@RequiredArgsConstructor
public class RegisterItemUseCase {

    private final ItemService itemService;
    private final ImageService imageService;

    public void execute(Member member, ItemRequest.Create request, MultipartFile file){
        member.validateAdmin();
        ItemType itemType = ItemType.fromKey(request.getItemTypeKey());
        // insert image
        Image image = null;
        if(file != null){
            image = imageService.uploadItemImage(file, itemType);
        }
        // register item
        Item item = itemService.registerItem(request, image);
    }

}
