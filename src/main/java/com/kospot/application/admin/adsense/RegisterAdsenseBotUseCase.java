package com.kospot.application.admin.adsense;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.application.service.MemberService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class RegisterAdsenseBotUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberService memberService;

    public void execute(String username, Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        member.validateAdmin();
        memberService.registerAdsenseBot(username);
    }

}
