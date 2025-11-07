package com.kospot.application.game.roadview.history.usecase;

import com.kospot.domain.game.adaptor.RoadViewGameAdaptor;
import com.kospot.domain.game.entity.RoadViewGame;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.gamerank.service.GameRankService;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.game.dto.response.RoadViewGameHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecentThreeRoadViewGamesUseCase {

    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final GameRankAdaptor gameRankAdaptor;
    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final GameRankService gameRankService;

    public RoadViewGameHistoryResponse.RecentThree execute(Member member) {
        // 최근 3개 게임 기록 조회
        List<RoadViewGame> games = roadViewGameAdaptor.queryRecentThreeGamesByMember(member);

        // 랭크 정보 조회
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW);

        // 전체 랭킹 통계 조회
        long totalRankCount = gameRankAdaptor.queryTotalRankCountByGameMode(GameMode.ROADVIEW);
        long higherRankCount = gameRankAdaptor.queryHigherRankCountByGameModeAndRatingScore(
                GameMode.ROADVIEW,
                gameRank.getRatingScore()
        );

        // 상위 퍼센트 계산
        double rankPercentage = gameRankService.calculateRankPercentage(higherRankCount, totalRankCount);

        // 통계 정보 조회
        MemberStatistic statistic = memberStatisticAdaptor.queryByMemberFetchModeStatistics(member);

        return RoadViewGameHistoryResponse.RecentThree.from(gameRank, rankPercentage, statistic, games);
    }
}

