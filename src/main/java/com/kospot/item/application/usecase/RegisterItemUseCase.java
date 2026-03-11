package com.kospot.item.application.usecase;

import com.kospot.image.domain.entity.Image;
import com.kospot.image.application.service.ImageService;
import com.kospot.item.domain.entity.Item;
import com.kospot.item.domain.vo.ItemType;
import com.kospot.item.presentation.dto.request.ItemRequest;
import com.kospot.item.application.service.ItemService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@UseCase
@Transactional
@RequiredArgsConstructor
public class RegisterItemUseCase {

    private final MemberAdaptor memberAdaptor;
    private final ItemService itemService;
    private final ImageService imageService;

    public void execute(Long memberId, ItemRequest.Create request, MultipartFile file){
        Member member = memberAdaptor.queryById(memberId);
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
