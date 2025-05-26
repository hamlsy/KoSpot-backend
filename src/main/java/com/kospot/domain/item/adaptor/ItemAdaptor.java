package com.kospot.domain.item.adaptor;

import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.entity.ItemType;
import com.kospot.domain.item.repository.ItemRepository;
import com.kospot.domain.member.entity.Member;
import com.kospot.global.exception.object.domain.ItemHandler;
import com.kospot.global.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;

import com.kospot.presentation.item.dto.response.ItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemAdaptor {

    private final ItemRepository repository;

    public List<Item> queryAllByItemType(ItemType itemType) {
        return repository.findAllByItemType(itemType);
    }

    public List<Item> queryAvailableItemsByItemTypeFetchImage(ItemType itemType) {
        return repository.findAvailableItemsByItemTypeFetchImage(itemType);
    }

    public List<ItemResponse> queryAvailableItemsByWithOwnersByFetchImage(Member member, ItemType itemType){
        return repository.findAvailableItemsWithOwnersByTypeFetchImage(member, itemType);
    }

    public Item queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

    public Item queryByIdFetchImage(Long id) {
        return repository.findByIdFetchImage(id).orElseThrow(
                () -> new ItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

}
