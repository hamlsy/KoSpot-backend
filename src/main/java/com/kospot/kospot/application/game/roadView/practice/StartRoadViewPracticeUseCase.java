package com.kospot.kospot.application.game.roadView.practice;

import com.kospot.kospot.domain.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.AESService;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class StartRoadViewPracticeUseCase {

    private final RoadViewGameService roadViewGameService;
    private final AESService aesService;

    public StartGameResponse.RoadView execute(Member member, String sidoKey){
        RoadViewGame game = roadViewGameService.startPracticeGame(member, sidoKey);
        return getEncryptedRoadViewGameResponse(game);
    }

    //encrypt -> response
    private StartGameResponse.RoadView getEncryptedRoadViewGameResponse(RoadViewGame game) {
        return StartGameResponse.RoadView.builder()
                .gameId(aesService.toEncryptString(game.getId()))
                .targetLat(aesService.toEncryptString(game.getTargetLat()))
                .targetLng(aesService.toEncryptString(game.getTargetLng()))
                .build();
    }



}
