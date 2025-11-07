package com.kospot.application.member;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
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
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberProfileUseCase {

    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final GameRankAdaptor gameRankAdaptor;

    public MemberProfileResponse execute(Member member) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
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
                .currentStreak(statistic.getCurrentStreak())
                .statistics(buildGameStatistics(statistic))
                .rankInfo(buildRankInfo(member))
                .build();
    }

    private GameStatistics buildGameStatistics(MemberStatistic statistic) {
        return GameStatistics.builder()
                .roadView(RoadViewGameStats.builder()
                        .practice(GameModeStats.builder()
                                .totalGames(statistic.getRoadviewPracticeGames())
                                .averageScore(statistic.getRoadviewPracticeAvgScore())
                                .build())
                        .rank(GameModeStats.builder()
                                .totalGames(statistic.getRoadviewRankGames())
                                .averageScore(statistic.getRoadviewRankAvgScore())
                                .build())
                        .multi(MultiGameStats.builder()
                                .totalGames(statistic.getRoadviewMultiGames())
                                .averageScore(statistic.getRoadviewMultiAvgScore())
                                .firstPlaceCount(statistic.getRoadviewMultiFirstPlace())
                                .secondPlaceCount(statistic.getRoadviewMultiSecondPlace())
                                .thirdPlaceCount(statistic.getRoadviewMultiThirdPlace())
                                .build())
                        .build())
                .photo(PhotoGameStats.builder()
                        .practice(GameModeStats.builder()
                                .totalGames(statistic.getPhotoPracticeGames())
                                .averageScore(statistic.getPhotoPracticeAvgScore())
                                .build())
                        .rank(GameModeStats.builder()
                                .totalGames(statistic.getPhotoRankGames())
                                .averageScore(statistic.getPhotoRankAvgScore())
                                .build())
                        .multi(MultiGameStats.builder()
                                .totalGames(statistic.getPhotoMultiGames())
                                .averageScore(statistic.getPhotoMultiAvgScore())
                                .firstPlaceCount(statistic.getPhotoMultiFirstPlace())
                                .secondPlaceCount(statistic.getPhotoMultiSecondPlace())
                                .thirdPlaceCount(statistic.getPhotoMultiThirdPlace())
                                .build())
                        .build())
                .bestScore(statistic.getBestScore())
                .build();
    }

    private RankInfo buildRankInfo(Member member) {
        GameRank roadViewRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW);
        GameRank photoRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.PHOTO);

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
}

