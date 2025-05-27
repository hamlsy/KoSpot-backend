package com.kospot.application.game.roadView.rank.usecase;

import com.kospot.presentation.game.dto.response.StartGameResponse;
import com.kospot.domain.game.entity.GameMode;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.service.AESService;
import com.kospot.domain.game.service.RoadViewGameService;
import com.kospot.domain.gameRank.adaptor.GameRankAdaptor;
import com.kospot.domain.gameRank.entity.GameRank;
import com.kospot.domain.gameRank.service.GameRankService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
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
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW);
        gameRankService.applyPenaltyForAbandon(gameRank);

        return getEncryptedRoadViewGameResponse(member, game);
    }

    //encrypt -> response
    private StartGameResponse.RoadView getEncryptedRoadViewGameResponse(Member member, RoadViewGame game) {
        return StartGameResponse.RoadView.builder()
                .gameId(aesService.toEncryptString(game.getId()))
                .targetLat(aesService.toEncryptString(game.getTargetLat()))
                .targetLng(aesService.toEncryptString(game.getTargetLng()))
                .markerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                .build();
    }

}
