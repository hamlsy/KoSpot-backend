package com.kospot.application.member;

import com.kospot.domain.gamerank.service.GameRankService;
import com.kospot.domain.image.service.ImageService;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.service.MemberService;
import com.kospot.domain.member.service.MemberStatisticService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class RegisterSocialMemberUseCase {

    private final MemberService memberService;
    private final MemberStatisticService memberStatisticService;
    private final GameRankService gameRankService;
    private final ImageService imageService;

    public Member execute(String username, String email) {
        Member member = memberService.initializeMember(username, email);
        memberStatisticService.initializeStatistic(member);
        gameRankService.initGameRank(member);
        //todo 기본 마커 이미지

        return member;
    }

}
