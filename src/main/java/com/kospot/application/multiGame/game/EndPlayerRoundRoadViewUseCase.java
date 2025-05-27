package com.kospot.application.multiGame.game;

import com.kospot.domain.multiGame.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multiGame.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multiGame.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multiGame.gameRound.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multiGame.gameRound.entity.RoadViewGameRound;
import com.kospot.domain.multiGame.gameRound.service.RoadViewGameRoundService;
import com.kospot.domain.multiGame.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multiGame.submission.service.RoadViewPlayerSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multiGame.round.dto.response.RoadViewRoundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class EndPlayerRoundRoadViewUseCase {

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final GamePlayerService gamePlayerService;
    private final RoadViewPlayerSubmissionService roadViewPlayerSubmissionService;

    public RoadViewRoundResponse.PlayerResult execute(Long gameId, Long roundId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchPlayerSubmissionAndPlayers(roundId);
        //todo 제출 못 한 플레이어 0점처리 - service

        // 플레이어 간 거리 순으로 순위 점수 처리 - service
        List<RoadViewPlayerSubmission> submission = roadViewPlayerSubmissionService.updateRankAndScore(round.getRoadViewPlayerSubmissions());

        // 라운드 종료 처리 및 전체 순위 처리(이전 점수는 프론트가 기억) - service
        roadViewGameRoundService.endGameRound(round);

        List<GamePlayer> players = gamePlayerAdaptor.queryByMultiRoadViewGameId(gameId);
        List<GamePlayer> updatedPlayers = gamePlayerService.updateTotalRank(players);

        // 라운드 결과 response - dto convert, 각 플레이어가 제출한 좌표 포함
        return RoadViewRoundResponse.PlayerResult.from(
                round,
                submission,
                updatedPlayers
        );
    }

}
