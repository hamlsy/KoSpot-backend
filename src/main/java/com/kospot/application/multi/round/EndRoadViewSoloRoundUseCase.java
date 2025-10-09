package com.kospot.application.multi.round;

import com.kospot.domain.multi.gamePlayer.adaptor.GamePlayerAdaptor;
import com.kospot.domain.multi.gamePlayer.entity.GamePlayer;
import com.kospot.domain.multi.gamePlayer.service.GamePlayerService;
import com.kospot.domain.multi.round.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.domain.multi.round.entity.RoadViewGameRound;
import com.kospot.domain.multi.round.service.RoadViewGameRoundService;
import com.kospot.domain.multi.submission.entity.roadView.RoadViewPlayerSubmission;
import com.kospot.domain.multi.submission.service.RoadViewSoloSubmissionService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.multi.round.dto.response.RoadViewRoundResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class EndRoadViewSoloRoundUseCase {

    private final RoadViewGameRoundAdaptor roadViewGameRoundAdaptor;
    private final RoadViewGameRoundService roadViewGameRoundService;
    private final GamePlayerAdaptor gamePlayerAdaptor;
    private final GamePlayerService gamePlayerService;
    private final RoadViewSoloSubmissionService roadViewSoloSubmissionService;

    public RoadViewRoundResponse.PlayerResult execute(Long gameId, Long roundId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryByIdFetchPlayerSubmissionAndPlayers(roundId);
        //todo 제출 못 한 플레이어 0점처리 - service

        // 플레이어 간 거리 순으로 순위 점수 처리 - service
        List<RoadViewPlayerSubmission> submission = roadViewSoloSubmissionService.updateRankAndScore(round.getRoadViewPlayerSubmissions());

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
