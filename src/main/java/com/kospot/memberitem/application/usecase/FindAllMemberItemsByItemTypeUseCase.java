package com.kospot.memberitem.application.usecase;

import com.kospot.item.domain.vo.ItemType;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.memberitem.application.adaptor.MemberItemAdaptor;
import com.kospot.common.annotation.usecase.UseCase;

import com.kospot.memberitem.presentation.response.MemberItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllMemberItemsByItemTypeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberItemAdaptor memberItemAdaptor;

    public List<MemberItemResponse> execute(Long memberId, String itemType) {
        Member member = memberAdaptor.queryById(memberId);
        return  memberItemAdaptor.queryByMemberAndItemTypeFetch(member, ItemType.fromKey(itemType));
    }

}
