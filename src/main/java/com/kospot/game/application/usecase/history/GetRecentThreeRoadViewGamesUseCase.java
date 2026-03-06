package com.kospot.game.application.usecase.history;

import com.kospot.game.application.adaptor.RoadViewGameAdaptor;
import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.game.domain.vo.GameMode;
import com.kospot.gamerank.application.adaptor.GameRankAdaptor;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.application.service.GameRankService;
import com.kospot.statistic.application.adaptor.MemberStatisticAdaptor;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.statistic.domain.entity.GameModeStatistic;
import com.kospot.statistic.domain.entity.MemberStatistic;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.game.presentation.dto.response.RoadViewGameHistoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRecentThreeRoadViewGamesUseCase {

    private final MemberAdaptor memberAdaptor;
    private final RoadViewGameAdaptor roadViewGameAdaptor;
    private final GameRankAdaptor gameRankAdaptor;
    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final GameRankService gameRankService;

    public RoadViewGameHistoryResponse.RecentThree execute(Long memberId) {
        Member member = memberAdaptor.queryById(memberId);
        // 최근 3개 게임 기록 조회
        List<RoadViewGame> games = roadViewGameAdaptor.queryRecentThreeGamesByMember(member);

        // 랭크 정보 조회
        GameRank gameRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW);

        // 통계 정보 조회
        MemberStatistic statistic = memberStatisticAdaptor.queryByMemberFetchModeStatistics(member);

        GameModeStatistic roadViewStatistic = statistic.findModeStatistic(GameMode.ROADVIEW);

        // 전체 랭킹 통계 조회
//        long totalRankCount = gameRankAdaptor.queryTotalRankCountByGameMode(GameMode.ROADVIEW);
        long totalRankCount = roadViewStatistic.getRank().getGames();
        long higherRankCount = gameRankAdaptor.queryHigherRankCountByGameModeAndRatingScore(
                GameMode.ROADVIEW,
                gameRank.getRatingScore()
        );

        // 상위 퍼센트 계산
        double rankPercentage = gameRankService.calculateRankPercentage(higherRankCount, totalRankCount);

        return RoadViewGameHistoryResponse.RecentThree.from(gameRank, rankPercentage, statistic, games);
    }
}

