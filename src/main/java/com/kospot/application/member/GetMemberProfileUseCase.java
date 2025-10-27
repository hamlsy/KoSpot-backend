package com.kospot.application.member;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.member.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.memberitem.repository.MemberItemRepository;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.member.dto.response.MemberProfileResponse;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics.SingleGameStats;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics.SingleGameStats.GameModeStats;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.GameStatistics.MultiGameStats;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.RankInfo;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.RankInfo.RoadViewRank;
import com.kospot.presentation.member.dto.response.MemberProfileResponse.ItemInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberProfileUseCase {

    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final MemberItemRepository memberItemRepository;
    private final GameRankAdaptor gameRankAdaptor;

    public MemberProfileResponse execute(Member member) {
        MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
        
        return MemberProfileResponse.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .currentPoint(member.getPoint())
                .joinedAt(member.getCreatedDate())
                .lastPlayedAt(statistic.getLastPlayedAt())
                .currentStreak(statistic.getCurrentStreak())
                .statistics(buildGameStatistics(statistic))
                .rankInfo(buildRankInfo(member))
                .itemInfo(buildItemInfo(member))
                .build();
    }

    private GameStatistics buildGameStatistics(MemberStatistic statistic) {
        return GameStatistics.builder()
                .singleGame(SingleGameStats.builder()
                        .practice(GameModeStats.builder()
                                .totalGames(statistic.getSinglePracticeGames())
                                .averageScore(statistic.getSinglePracticeAvgScore())
                                .build())
                        .rank(GameModeStats.builder()
                                .totalGames(statistic.getSingleRankGames())
                                .averageScore(statistic.getSingleRankAvgScore())
                                .build())
                        .build())
                .multiGame(MultiGameStats.builder()
                        .totalGames(statistic.getMultiGames())
                        .averageScore(statistic.getMultiAvgScore())
                        .firstPlaceCount(statistic.getMultiFirstPlace())
                        .secondPlaceCount(statistic.getMultiSecondPlace())
                        .thirdPlaceCount(statistic.getMultiThirdPlace())
                        .build())
                .bestScore(statistic.getBestScore())
                .build();
    }

    private RankInfo buildRankInfo(Member member) {
        GameRank roadViewRank = gameRankAdaptor.queryByMemberAndGameMode(member, GameMode.ROADVIEW);

        return RankInfo.builder()
                .roadViewRank(RoadViewRank.builder()
                        .tier(roadViewRank.getRankTier())
                        .level(roadViewRank.getRankLevel())
                        .ratingScore(roadViewRank.getRatingScore())
                        .build())
                .build();
    }

    private ItemInfo buildItemInfo(Member member) {
        long totalItems = memberItemRepository.countByMember(member);
        long equippedItems = memberItemRepository.countEquippedByMember(member);

        return ItemInfo.builder()
                .totalItems((int) totalItems)
                .equippedItems((int) equippedItems)
                .build();
    }
}

