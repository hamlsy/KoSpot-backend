package com.kospot.kospot.domain.item.service;

import com.kospot.kospot.domain.item.dto.request.ItemRequest;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.item.repository.ItemRepository;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.service.AwsS3Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final AwsS3Service awsS3Service;

    //todo optimize image upload transaction
    public void registerItem(Member member, ItemRequest.Create request){
        member.validateAdmin();
        ItemType itemType = ItemType.fromKey(request.getItemTypeKey());
        String imageUrl = awsS3Service.uploadImage(request.getImage());

        Item item = Item.create(
                request.getName(),
                request.getDescription(),
                itemType,
                request.getPrice(),
                imageUrl
        );

        itemRepository.save(item);
    }


}
