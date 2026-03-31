package com.kospot.member.application.usecase;

import com.kospot.gamerank.application.service.GameRankService;
import com.kospot.item.application.adaptor.ItemAdaptor;
import com.kospot.item.domain.entity.Item;
import com.kospot.item.domain.vo.ItemType;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.application.service.MemberService;
import com.kospot.member.domain.entity.Member;
import com.kospot.member.domain.exception.MemberErrorStatus;
import com.kospot.member.domain.exception.MemberHandler;
import com.kospot.memberitem.application.service.MemberItemService;
import com.kospot.memberitem.domain.entity.MemberItem;
import com.kospot.statistic.application.service.MemberStatisticService;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.common.security.dto.JwtToken;
import com.kospot.common.security.service.TokenService;
import com.kospot.member.infrastructure.redis.service.MemberProfileRedisService;
import com.kospot.common.slack.SlackNotifier;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@Transactional
@RequiredArgsConstructor
public class SignUpUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberService memberService;
    private final MemberStatisticService memberStatisticService;
    private final GameRankService gameRankService;
    private final MemberItemService memberItemService;
    private final ItemAdaptor itemAdaptor;
    private final TokenService tokenService;
    private final MemberProfileRedisService memberProfileRedisService;
    private final SlackNotifier slackNotifier;
    private final BCryptPasswordEncoder passwordEncoder;

    public JwtToken execute(String email, String rawPassword) {
        if (memberAdaptor.existsByEmail(email)) {
            throw new MemberHandler(MemberErrorStatus.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        Member member = memberService.initializeLocalMember(email, encodedPassword);

        memberStatisticService.initializeStatistic(member);
        gameRankService.initGameRank(member);

        Item defaultMarker = itemAdaptor.queryDefaultItemByItemType(ItemType.MARKER);
        MemberItem memberItem = memberItemService.purchaseItem(member, defaultMarker);
        memberItemService.equipItem(member, memberItem);

        memberProfileRedisService.saveProfile(member.getId(), member.getNickname(),
                defaultMarker.getImage().getImageUrl());

        slackNotifier.sendRegistrationAlert(member.getId(), email);

        return tokenService.generateTokenByMember(member);
    }
}
