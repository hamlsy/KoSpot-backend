package com.kospot.memberitem.application.adaptor;

import com.kospot.item.domain.vo.ItemType;
import com.kospot.item.domain.entity.Item;
import com.kospot.member.domain.entity.Member;
import com.kospot.memberitem.domain.entity.MemberItem;
import com.kospot.memberitem.infrastructure.persistence.MemberItemRepository;
import com.kospot.common.exception.object.domain.MemberItemHandler;
import com.kospot.common.exception.payload.code.ErrorStatus;
import com.kospot.common.annotation.adaptor.Adaptor;
import com.kospot.memberitem.presentation.response.MemberItemResponse;
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

    public List<Long> queryEquippedItemIdsByMemberId(Long memberId) {
        return repository.findEquippedIdsByMemberId(memberId);
    }

    public List<Long> queryOwnedItemIdsByMemberId(Long memberId) {
        return repository.findOwnedIdsByMemberId(memberId);
    }

    public boolean existsByMemberAndItem(Member member, Item item) {
        return repository.existsByMemberAndItem(member, item);
    }

}
