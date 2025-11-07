package com.kospot.application.member;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.entity.GameModeStatistic;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.member.dto.response.MemberProfileResponse;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics.RoadViewGameStats;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics.PhotoGameStats;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics.GameModeStats;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics.MultiGameStats;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.RankInfo;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.RankInfo.RoadViewRank;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.RankInfo.PhotoRank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true) // todo error handler
public class GetMemberProfileUseCase {

    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final GameRankAdaptor gameRankAdaptor;

    public MemberProfileResponse execute(Member member) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMemberFetchModeStatistics(member);

        String profileImageUrl = member.getEquippedMarkerImage() != null 
                ? member.getEquippedMarkerImage().getImageUrl() 
                : null;
        
        return MemberProfileResponse.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .profileImageUrl(profileImageUrl)
                .currentPoint(member.getPoint())
                .joinedAt(member.getCreatedDate())
                .lastPlayedAt(statistic.getLastPlayedAt())
                .currentStreak(statistic.getPlayStreak().getCurrentStreak())
                .statistics(buildGameStatistics(statistic))
                .rankInfo(buildRankInfo(member))
                .build();
    }

    private GameStatistics buildGameStatistics(MemberStatistic statistic) {
        List<GameModeStatistic> modeStatistics = statistic.getModeStatistics();
        
        GameModeStatistic roadViewStatistic = findModeStatistic(modeStatistics, GameMode.ROADVIEW);
        GameModeStatistic photoStatistic = findModeStatistic(modeStatistics, GameMode.PHOTO);
        
        return GameStatistics.builder()
                .roadView(RoadViewGameStats.builder()
                        .practice(GameModeStats.builder()
                                .totalGames(roadViewStatistic.getPractice().getGames())
                                .averageScore(roadViewStatistic.getPractice().getAvgScore())
                                .build())
                        .rank(GameModeStats.builder()
                                .totalGames(roadViewStatistic.getRank().getGames())
                                .averageScore(roadViewStatistic.getRank().getAvgScore())
                                .build())
                        .multi(MultiGameStats.builder()
                                .totalGames(roadViewStatistic.getMulti().getGames())
                                .averageScore(roadViewStatistic.getMulti().getAvgScore())
                                .firstPlaceCount(roadViewStatistic.getMulti().getFirstPlace())
                                .secondPlaceCount(roadViewStatistic.getMulti().getSecondPlace())
                                .thirdPlaceCount(roadViewStatistic.getMulti().getThirdPlace())
                                .build())
                        .build())
                .photo(PhotoGameStats.builder()
                        .practice(GameModeStats.builder()
                                .totalGames(photoStatistic.getPractice().getGames())
                                .averageScore(photoStatistic.getPractice().getAvgScore())
                                .build())
                        .rank(GameModeStats.builder()
                                .totalGames(photoStatistic.getRank().getGames())
                                .averageScore(photoStatistic.getRank().getAvgScore())
                                .build())
                        .multi(MultiGameStats.builder()
                                .totalGames(photoStatistic.getMulti().getGames())
                                .averageScore(photoStatistic.getMulti().getAvgScore())
                                .firstPlaceCount(photoStatistic.getMulti().getFirstPlace())
                                .secondPlaceCount(photoStatistic.getMulti().getSecondPlace())
                                .thirdPlaceCount(photoStatistic.getMulti().getThirdPlace())
                                .build())
                        .build())
                .bestScore(calculateBestScore(modeStatistics))
                .build();
    }

    private GameModeStatistic findModeStatistic(List<GameModeStatistic> modeStatistics, GameMode gameMode) {
        return modeStatistics.stream()
                .filter(stat -> stat.getGameMode() == gameMode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GameModeStatistic not found: " + gameMode));
    }

    private double calculateBestScore(List<GameModeStatistic> modeStatistics) {
        return modeStatistics.stream()
                .flatMap(stat -> java.util.stream.Stream.of(
                        stat.getPractice().getAvgScore(),
                        stat.getRank().getAvgScore(),
                        stat.getMulti().getAvgScore()
                ))
                .filter(score -> score > 0)
                .max(Double::compareTo)
                .orElse(0.0);
    }

    private RankInfo buildRankInfo(Member member) {
        List<GameRank> ranks = gameRankAdaptor.queryAllByMember(member);
        GameRank roadViewRank = findModeRank(ranks, GameMode.ROADVIEW);
        GameRank photoRank = findModeRank(ranks, GameMode.PHOTO);

        return RankInfo.builder()
                .roadViewRank(RoadViewRank.builder()
                        .tier(roadViewRank.getRankTier())
                        .level(roadViewRank.getRankLevel())
                        .ratingScore(roadViewRank.getRatingScore())
                        .build())
                .photoRank(PhotoRank.builder()
                        .tier(photoRank.getRankTier())
                        .level(photoRank.getRankLevel())
                        .ratingScore(photoRank.getRatingScore())
                        .build())
                .build();
    }

    private GameRank findModeRank(List<GameRank> ranks , GameMode gameMode) {
        return ranks.stream()
                .filter(stat -> stat.getGameMode() == gameMode)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("GameRank not found: " + gameMode));
    }
}

