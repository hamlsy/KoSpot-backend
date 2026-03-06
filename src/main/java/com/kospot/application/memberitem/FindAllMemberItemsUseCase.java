package com.kospot.application.memberitem;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.memberitem.adaptor.MemberItemAdaptor;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.presentation.memberitem.dto.response.MemberItemResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindAllMemberItemsUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberItemAdaptor memberItemAdaptor;

    public List<MemberItemResponse> execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        return memberItemAdaptor.queryAllByMemberFetch(member);
    }

}

