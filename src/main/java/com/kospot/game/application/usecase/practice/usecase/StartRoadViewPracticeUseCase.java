package com.kospot.game.application.usecase.practice.usecase;

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

    public StartGameResponse.RoadView execute(Long memberId, String sidoKey) {
        Member member = memberAdaptor.queryById(memberId);
        RoadViewGame game = roadViewGameService.startPracticeGame(member, sidoKey);
        MemberProfileRedisAdaptor.MemberProfileView cachedProfile = memberProfileRedisAdaptor.findProfile(member.getId());
        String markerImageUrl = cachedProfile.markerImageUrl();
        return getEncryptedRoadViewGameResponse(member, game, markerImageUrl);
    }

    //encrypt -> response
    private StartGameResponse.RoadView getEncryptedRoadViewGameResponse(Member member, RoadViewGame game, String markerImageUrl) {
        return StartGameResponse.RoadView.builder()
                .poiName(game.getCoordinate().getPoiName())
                .gameId(game.getId())
                .targetLat(aesService.toEncryptString(game.getCoordinate().getLat()))
                .targetLng(aesService.toEncryptString(game.getCoordinate().getLng()))
                .markerImageUrl(markerImageUrl)
                .build();
    }


}
