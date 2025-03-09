package com.kospot.kospot.domain.item.service;

import com.kospot.kospot.domain.item.dto.request.ItemRequest;
import com.kospot.kospot.domain.item.entity.Item;
import com.kospot.kospot.domain.item.repository.ItemRepository;
import com.kospot.kospot.domain.member.entity.Member;
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

    //todo set Amazon
    public void registerItem(Member member, ItemRequest.Create request){
        member.validateAdmin();

        itemRepository.save(item);
    }


}
