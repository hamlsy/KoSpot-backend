package com.kospot.application.game.roadview.practice.usecase;

import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.presentation.game.dto.response.StartGameResponse;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.service.AESService;
import com.kospot.domain.game.service.RoadViewGameService;
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
