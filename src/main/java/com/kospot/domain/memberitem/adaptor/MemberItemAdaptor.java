package com.kospot.domain.memberitem.adaptor;

import com.kospot.domain.item.vo.ItemType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberitem.entity.MemberItem;
import com.kospot.domain.memberitem.repository.MemberItemRepository;
import com.kospot.infrastructure.exception.object.domain.MemberItemHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.presentation.memberitem.dto.response.MemberItemResponse;
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

    public MemberItem queryByIdFetchItemAndImage(Long id) {
        return repository.findByIdFetchItemAndImage(id).orElseThrow(
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

    public List<MemberItemResponse> queryAllByMemberFetch(Member member) {
        return repository.findAllByMemberFetch(member);
    }

}
