package com.kospot.kospot.domain.memberItem.service;

import com.kospot.kospot.domain.memberItem.repository.MemberItemRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MemberItemService {

    private final MemberItemRepository memberItemRepository;

    public void deleteAllByItemId(Long itemId){
        memberItemRepository.deleteAllByItemId(itemId);
    }

}
