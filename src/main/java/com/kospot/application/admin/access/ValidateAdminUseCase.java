package com.kospot.application.admin.access;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.exception.MemberErrorStatus;
import com.kospot.domain.member.exception.MemberHandler;
import com.kospot.domain.member.service.MemberService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ValidateAdminUseCase {

    private final MemberService memberService;

    public void execute(Member member) {
        memberService.validateAdmin(member);
    }

}
