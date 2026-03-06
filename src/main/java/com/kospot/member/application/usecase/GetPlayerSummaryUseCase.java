package com.kospot.member.application.usecase;

import com.kospot.game.domain.vo.GameMode;
import com.kospot.gamerank.application.adaptor.GameRankAdaptor;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.statistic.application.adaptor.MemberStatisticAdaptor;
import com.kospot.statistic.domain.entity.GameModeStatistic;
import com.kospot.statistic.domain.entity.MemberStatistic;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import com.kospot.member.presentation.response.PlayerSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPlayerSummaryUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final GameRankAdaptor gameRankAdaptor;
    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    public PlayerSummaryResponse execute(Long memberId) {
        // Member 조회 (equippedMarkerImage 포함)
        Member member = memberAdaptor.queryByIdFetchMarkerImage(memberId);

        // MemberStatistic 조회 (modeStatistics 포함)
        MemberStatistic statistic = memberStatisticAdaptor.queryByMemberIdFetchModeStatistics(memberId);

        // GameRank 목록 조회
        List<GameRank> ranks = gameRankAdaptor.queryAllByMember(member);

        // equippedMarkerImageUrl 추출
        String equippedMarkerImageUrl = memberProfileRedisAdaptor.findProfile(memberId).markerImageUrl();

        // PlayStreak 추출
        int playStreak = statistic.getPlayStreak() != null
                ? statistic.getPlayStreak().getCurrentStreak()
                : 0;

        // GameModeStatistic 추출
        List<GameModeStatistic> modeStatistics = statistic.getModeStatistics();
        GameModeStatistic roadViewStatistic = findModeStatistic(modeStatistics, GameMode.ROADVIEW);
        GameModeStatistic photoStatistic = findModeStatistic(modeStatistics, GameMode.PHOTO);

        // GameRank 추출
        GameRank roadViewRank = findModeRank(ranks, GameMode.ROADVIEW);
        GameRank photoRank = findModeRank(ranks, GameMode.PHOTO);

        // Response 생성
        return PlayerSummaryResponse.builder()
                .nickname(member.getNickname())
                .playStreak(playStreak)
                .equippedMarkerImageUrl(equippedMarkerImageUrl)
                .joinedAt(member.getCreatedDate())
                .rankInfo(buildRankInfo(roadViewRank, photoRank, roadViewStatistic, photoStatistic))
                .multiGameStats(buildMultiGameStats(roadViewStatistic, photoStatistic))
                .build();
    }

    private GameModeStatistic findModeStatistic(List<GameModeStatistic> modeStatistics, GameMode gameMode) {
        return modeStatistics.stream()
                .filter(stat -> stat.getGameMode() == gameMode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GameModeStatistic not found: " + gameMode));
    }

    private GameRank findModeRank(List<GameRank> ranks, GameMode gameMode) {
        return ranks.stream()
                .filter(rank -> rank.getGameMode() == gameMode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GameRank not found: " + gameMode));
    }

    private PlayerSummaryResponse.RankInfo buildRankInfo(
            GameRank roadViewRank,
            GameRank photoRank,
            GameModeStatistic roadViewStatistic,
            GameModeStatistic photoStatistic) {
        return PlayerSummaryResponse.RankInfo.builder()
                .roadView(PlayerSummaryResponse.RankInfo.RoadViewRankInfo.builder()
                        .ratingScore(roadViewRank.getRatingScore())
                        .rankLevel(roadViewRank.getRankLevel())
                        .rankTier(roadViewRank.getRankTier())
                        .rankAvgScore(roadViewStatistic.getRank().getAvgScore())
                        .build())
                .photo(PlayerSummaryResponse.RankInfo.PhotoRankInfo.builder()
                        .ratingScore(photoRank.getRatingScore())
                        .rankLevel(photoRank.getRankLevel())
                        .rankTier(photoRank.getRankTier())
                        .rankAvgScore(photoStatistic.getRank().getAvgScore())
                        .build())
                .build();
    }

    private PlayerSummaryResponse.MultiGameStats buildMultiGameStats(
            GameModeStatistic roadViewStatistic,
            GameModeStatistic photoStatistic) {
        return PlayerSummaryResponse.MultiGameStats.builder()
                .roadView(PlayerSummaryResponse.MultiGameStats.RoadViewMultiStats.builder()
                        .totalGames(roadViewStatistic.getMulti().getGames())
                        .firstPlaceCount(roadViewStatistic.getMulti().getFirstPlace())
                        .build())
                .photo(PlayerSummaryResponse.MultiGameStats.PhotoMultiStats.builder()
                        .totalGames(photoStatistic.getMulti().getGames())
                        .firstPlaceCount(photoStatistic.getMulti().getFirstPlace())
                        .build())
                .build();
    }
}

