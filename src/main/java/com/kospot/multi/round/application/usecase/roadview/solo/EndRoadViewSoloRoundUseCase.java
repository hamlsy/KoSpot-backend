package com.kospot.multi.round.application.usecase.roadview.solo;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.multi.player.application.adaptor.GamePlayerAdaptor;
import com.kospot.multi.player.domain.entity.GamePlayer;
import com.kospot.multi.player.application.service.GamePlayerService;
import com.kospot.multi.round.application.adaptor.RoadViewGameRoundAdaptor;
import com.kospot.multi.round.entity.RoadViewGameRound;
import com.kospot.multi.round.application.roadview.RoadViewGameRoundService;
import com.kospot.multi.submission.application.roadview.RoadViewSubmissionAdaptor;
import com.kospot.multi.submission.entity.roadview.RoadViewSubmission;
import com.kospot.multi.submission.application.service.RoadViewSubmissionService;
import com.kospot.common.annotation.usecase.UseCase;

import com.kospot.multi.round.presentation.dto.response.RoadViewRoundResponse;
import com.kospot.multi.submission.infrastructure.redis.service.SubmissionRedisService;
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
    private final RoadViewSubmissionService roadViewSubmissionService;
    private final RoadViewSubmissionAdaptor roadViewSubmissionAdaptor;
    private final SubmissionRedisService submissionRedisService;

    public RoadViewRoundResponse.PlayerResult execute(Long gameId, Long roundId) {
        RoadViewGameRound round = roadViewGameRoundAdaptor.queryById(roundId);
        List<RoadViewSubmission> roadViewSubmissions = roadViewSubmissionAdaptor.queryByRoundIdFetchGamePlayer(roundId);

        //플레이어 간 거리 순으로 순위 점수 처리 - service
        List<RoadViewSubmission> submission = roadViewSubmissionService.calculatePlayerRankAndScore(roadViewSubmissions);

        // 라운드 종료 처리 및 전체 순위 처리 - service
        roadViewGameRoundService.endGameRound(round);

        List<GamePlayer> players = gamePlayerAdaptor.queryByMultiRoadViewGameIdWithMember(gameId);
        List<GamePlayer> updatedPlayers = gamePlayerService.updateTotalRank(players);

        // redis  정리
        submissionRedisService.cleanupRound(GameMode.ROADVIEW, roundId);

        return RoadViewRoundResponse.PlayerResult.from(
                round,
                submission,
                updatedPlayers
        );
    }

}
