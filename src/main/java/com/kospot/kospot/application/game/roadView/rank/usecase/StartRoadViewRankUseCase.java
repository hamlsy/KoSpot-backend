package com.kospot.kospot.application.game.roadView.rank.usecase;

import com.kospot.kospot.presentation.game.dto.response.StartGameResponse;
import com.kospot.kospot.domain.game.entity.GameMode;
import com.kospot.kospot.domain.game.entity.RoadViewGame;
import com.kospot.kospot.domain.game.service.AESService;
import com.kospot.kospot.domain.game.service.RoadViewGameService;
import com.kospot.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.kospot.domain.gameRank.entity.GameRank;
import com.kospot.kospot.domain.gameRank.service.GameRankService;
import com.kospot.kospot.domain.member.entity.Member;
import com.kospot.kospot.global.annotation.usecase.UseCase;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
@Transactional
public class StartRoadViewRankUseCase {

    private final RoadViewGameService roadViewGameService;
    private final GameRankAdaptor gameRankAdaptor;
    private final GameRankService gameRankService;
    private final AESService aesService;

    public StartGameResponse.RoadView execute(Member member){
        RoadViewGame game = roadViewGameService.startRankGame(member);
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameType(member, GameMode.ROADVIEW);
        gameRankService.applyPenaltyForAbandon(gameRank);

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
