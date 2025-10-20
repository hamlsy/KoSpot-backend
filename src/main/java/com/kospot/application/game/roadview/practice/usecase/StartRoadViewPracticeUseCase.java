package com.kospot.application.game.roadview.practice.usecase;

import com.kospot.presentation.game.dto.response.StartGameResponse;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.service.AESService;
import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class StartRoadViewPracticeUseCase {

    private final RoadViewGameService roadViewGameService;
    private final AESService aesService;

    public StartGameResponse.RoadView execute(Member member, String sidoKey){
        RoadViewGame game = roadViewGameService.startPracticeGame(member, sidoKey);
        return getEncryptedRoadViewGameResponse(member, game);
    }

    //encrypt -> response
    private StartGameResponse.RoadView getEncryptedRoadViewGameResponse(Member member, RoadViewGame game) {
        return StartGameResponse.RoadView.builder()
                .gameId(aesService.toEncryptString(game.getId()))
                .targetLat(aesService.toEncryptString(game.getCoordinate().getLat()))
                .targetLng(aesService.toEncryptString(game.getCoordinate().getLng()))
                .markerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                .build();
    }



}
