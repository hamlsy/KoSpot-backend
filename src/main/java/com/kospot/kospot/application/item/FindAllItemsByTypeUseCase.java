package com.kospot.kospot.application.item;

import com.kospot.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.kospot.domain.item.dto.response.ItemResponse;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindAllItemsByTypeUseCase {

    private final ItemAdaptor itemAdaptor;

    public List<ItemResponse.ItemDto> execute(String itemTypeKey) {
        ItemType itemType = ItemType.fromKey(itemTypeKey);
        List<Item> items = itemAdaptor.queryAllByItemType(itemType);
        return items.stream()
                .map(ItemResponse.ItemDto::from)
                .collect(Collectors.toList());
    }

}
