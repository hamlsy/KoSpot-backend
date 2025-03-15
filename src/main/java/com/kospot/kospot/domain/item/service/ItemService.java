package com.kospot.kospot.domain.item.service;

import com.kospot.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.presentation.item.dto.request.ItemRequest;
import com.kospot.kospot.domain.item.entity.Item;

import com.kospot.kospot.domain.item.repository.ItemRepository;
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
    private final ItemAdaptor itemAdaptor;

    //todo optimize image upload transaction
    // Item Create가 실패해도 S3에 이미지가 올라가는 문제 발생
    public Item registerItem(ItemRequest.Create request) {
        Item item = request.toEntity();

        return itemRepository.save(item);
    }

    public void deleteItemById(Long id) {
        itemRepository.deleteById(id);
    }

    public void deleteItemFromShop(Long id) {
        Item item = itemAdaptor.queryById(id);
        item.deleteFromShop();
    }

    public void restoreItemToShop(Long id) {
        Item item = itemAdaptor.queryById(id);
        item.restoreToShop();
    }

    public void updateItemInfo(ItemRequest.UpdateInfo request) {
        Item item = itemAdaptor.queryById(request.getItemId());
        item.updateItemInfo(
                request.getName(), request.getDescription(),
                ItemType.fromKey(request.getItemTypeKey()),
                request.getPrice(), request.getQuantity()
        );
    }
}
