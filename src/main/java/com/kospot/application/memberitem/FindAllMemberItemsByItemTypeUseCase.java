package com.kospot.application.memberitem;

import com.kospot.domain.item.vo.ItemType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberitem.adaptor.MemberItemAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;

import com.kospot.presentation.memberitem.dto.response.MemberItemResponse;
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
