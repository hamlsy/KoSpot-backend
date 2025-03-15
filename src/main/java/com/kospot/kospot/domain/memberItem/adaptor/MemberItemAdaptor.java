package com.kospot.kospot.domain.memberItem.adaptor;

import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.kospot.domain.memberItem.repository.MemberItemRepository;
import com.kospot.kospot.exception.object.domain.MemberItemHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import com.kospot.kospot.presentation.memberItem.dto.response.MemberItemResponse;
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

    public List<MemberItemResponse.MemberItemDto> queryByMemberAndItemTypeFetch(Member member, ItemType itemType) {
        return repository.findAllByMemberAndItemTypeFetch(member, itemType);
    }

}
