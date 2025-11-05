package com.kospot.application.member;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.service.MemberService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class SetNicknameUseCase {

    private final MemberService memberService;

    public void execute(Member member, String nickname) {
        memberService.setNickname(member, nickname);
    }

}
