package com.kospot.member.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.exception.MemberErrorStatus;
import com.kospot.member.domain.exception.MemberHandler;
import com.kospot.member.application.service.MemberService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.infrastructure.redis.domain.member.service.MemberProfileRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@UseCase
@Transactional
@RequiredArgsConstructor
public class UpdateNicknameUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberService memberService;

    //redis
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;
    private final MemberProfileRedisService memberProfileRedisService;

    public void execute(Long memberId, String nickname) {
        Member member = memberAdaptor.queryById(memberId);
        validateNicknameDuplication(nickname);
        memberService.updateNickname(member, nickname);

        //redis update
        if (memberProfileRedisAdaptor.findProfile(member.getId()) == null) {
            memberProfileRedisService.saveProfile(member.getId(), member.getNickname(), member.getEquippedMarkerImage().getImageUrl());
        }else{
            memberProfileRedisService.updateNickname(member.getId(), nickname);
        }

    }

    private void validateNicknameDuplication(String nickname) {
        if (memberAdaptor.existsByNickname(nickname)) {
            throw new MemberHandler(MemberErrorStatus.NICKNAME_ALREADY_EXISTS);
        }
    }
}
