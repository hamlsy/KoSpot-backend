package com.kospot.domain.memberItem.adaptor;

import com.kospot.domain.item.entity.ItemType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.domain.memberItem.repository.MemberItemRepository;
import com.kospot.exception.object.domain.MemberItemHandler;
import com.kospot.exception.payload.code.ErrorStatus;
import com.kospot.global.annotation.adaptor.Adaptor;
import com.kospot.presentation.memberItem.dto.response.MemberItemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberItemAdaptor {

    private final MemberItemRepository repository;

    public MemberItem queryById(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new MemberItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

    public MemberItem queryByIdFetchItem(Long id) {
        return repository.findByIdFetchItem(id).orElseThrow(
                () -> new MemberItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

    public MemberItem queryByItemIdFetchItem(Long itemId) {
        return repository.findByItemIdFetchItem(itemId).orElseThrow(
                () -> new MemberItemHandler(ErrorStatus.ITEM_NOT_FOUND)
        );
    }

    public List<MemberItemResponse> queryByMemberAndItemTypeFetch(Member member, ItemType itemType) {
        return repository.findAllByMemberAndItemTypeFetch(member, itemType);
    }


}
