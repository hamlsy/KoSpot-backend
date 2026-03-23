package com.kospot.game.application.usecase.practice.usecase;

import com.kospot.game.infrastructure.redis.adaptor.AnonymousPracticeTokenRedisAdaptor;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.game.presentation.dto.response.StartGameResponse;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.application.service.AESService;
import com.kospot.game.application.service.RoadViewGameService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class StartRoadViewPracticeUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameService roadViewGameService;
    private final AESService aesService;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;
    private final AnonymousPracticeTokenRedisAdaptor anonymousPracticeTokenRedisAdaptor;

    public StartGameResponse.RoadView execute(Long memberId, String sidoKey) {
        if (memberId != null) {
            return startAsLoggedIn(memberId, sidoKey);
        }
        return startAsAnonymous(sidoKey);
    }

    private StartGameResponse.RoadView startAsLoggedIn(Long memberId, String sidoKey) {
        Member member = memberAdaptor.queryById(memberId);
        RoadViewGame game = roadViewGameService.startPracticeGame(member, sidoKey);
        String markerImageUrl = memberProfileRedisAdaptor.findProfile(memberId).markerImageUrl();
        return buildResponse(game, markerImageUrl, null);
    }

    private StartGameResponse.RoadView startAsAnonymous(String sidoKey) {
        RoadViewGame game = roadViewGameService.startAnonymousPracticeGame(sidoKey);
        String practiceToken = anonymousPracticeTokenRedisAdaptor.generateAndStore(game.getId());
        return buildResponse(game, null, practiceToken);
    }

    private StartGameResponse.RoadView buildResponse(RoadViewGame game,
                                                      String markerImageUrl,
                                                      String practiceToken) {
        return StartGameResponse.RoadView.builder()
                .gameId(game.getId())
                .poiName(game.getCoordinate().getPoiName())
                .targetLat(aesService.toEncryptString(game.getCoordinate().getLat()))
                .targetLng(aesService.toEncryptString(game.getCoordinate().getLng()))
                .markerImageUrl(markerImageUrl)
                .practiceToken(practiceToken)
                .build();
    }
}
