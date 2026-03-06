package com.kospot.member.application.usecase;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.member.service.MemberProfileRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CacheMemberProfileUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberProfileRedisService memberProfileRedisService;

    public void execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        String nickname = member.getNickname();
        String markerImageUrl = member.getEquippedMarkerImage().getImageUrl();
        memberProfileRedisService.saveProfile(memberId, nickname, markerImageUrl);
    }

}
