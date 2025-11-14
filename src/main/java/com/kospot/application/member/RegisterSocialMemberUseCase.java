package com.kospot.application.member;

import com.kospot.domain.gamerank.service.GameRankService;
import com.kospot.domain.item.adaptor.ItemAdaptor;
import com.kospot.domain.item.entity.Item;
import com.kospot.domain.item.vo.ItemType;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.service.MemberService;
import com.kospot.domain.statistic.service.MemberStatisticService;
import com.kospot.domain.memberitem.entity.MemberItem;
import com.kospot.domain.memberitem.service.MemberItemService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.member.service.MemberProfileRedisService;
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
    private final MemberItemService memberItemService;
    private final ItemAdaptor itemAdaptor;

    //redis
    private final MemberProfileRedisService memberProfileRedisService;

    public Member execute(String username, String email) {
        Member member = memberService.initializeMember(username, email);

        memberStatisticService.initializeStatistic(member);

        gameRankService.initGameRank(member);

        Item defaultMarker = itemAdaptor.queryDefaultItemByItemType(ItemType.MARKER);

        MemberItem memberItem = memberItemService.purchaseItem(member, defaultMarker);
        memberItemService.equipItem(member, memberItem);

        // redis profile cache update
        memberProfileRedisService.saveProfile(member.getId(), member.getNickname(), defaultMarker.getImage().getImageUrl());

        return member;
    }

}
