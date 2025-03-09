package com.kospot.kospot.domain.item.adaptor;

import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.item.repository.ItemRepository;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;

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

    public List<Item> queryAllByItemType(ItemType itemType){
        return repository.findAllByItemType(itemType);
    }

}
