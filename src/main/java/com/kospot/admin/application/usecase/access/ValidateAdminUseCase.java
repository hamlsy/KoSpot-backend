package com.kospot.admin.application.usecase.access;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.application.service.MemberService;
import com.kospot.common.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ValidateAdminUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberService memberService;

    public void execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        memberService.validateAdmin(member);
    }

}
