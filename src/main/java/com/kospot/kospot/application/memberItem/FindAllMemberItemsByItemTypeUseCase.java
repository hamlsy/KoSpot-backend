package com.kospot.kospot.application.memberItem;

import com.kospot.kospot.domain.item.entity.ItemType;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.domain.memberItem.adaptor.MemberItemAdaptor;
import com.kospot.kospot.domain.memberItem.entity.MemberItem;
import com.kospot.kospot.global.annotation.usecase.UseCase;

import com.kospot.kospot.presentation.memberItem.dto.response.MemberItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllMemberItemsByItemTypeUseCase {

    private final MemberItemAdaptor memberItemAdaptor;

    public List<MemberItemResponse> execute(Member member, String itemType) {
        return  memberItemAdaptor.queryByMemberAndItemTypeFetch(member, ItemType.fromKey(itemType));
    }

}
