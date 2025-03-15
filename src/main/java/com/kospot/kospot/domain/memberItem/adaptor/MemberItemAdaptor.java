package com.kospot.kospot.domain.memberItem.adaptor;

import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.kospot.domain.memberItem.repository.MemberItemRepository;
import com.kospot.kospot.exception.object.domain.MemberItemHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberItemAdaptor {

    private final MemberItemRepository memberItemRepository;

    public MemberItem queryById(Long id) {
        return memberItemRepository.findById(id).orElseThrow(
                () -> new MemberItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

    public MemberItem queryByIdFetchItem(Long id) {
        return memberItemRepository.findByIdFetchItem(id).orElseThrow(
                () -> new MemberItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

}
