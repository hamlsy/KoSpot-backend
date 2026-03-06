package com.kospot.item.application.adaptor;

import com.kospot.item.domain.entity.Item;
import com.kospot.item.domain.vo.ItemType;
import com.kospot.item.infrastructure.persistence.ItemRepository;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.exception.object.domain.ItemHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.common.annotation.adaptor.Adaptor;

import com.kospot.item.presentation.dto.response.ItemResponse;
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

    public Item queryDefaultItemByItemType(ItemType itemType) {
        return repository.findDefaultItemByType(itemType).orElseThrow(
                () -> new ItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

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
