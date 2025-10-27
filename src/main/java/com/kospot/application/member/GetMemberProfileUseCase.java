package com.kospot.application.member;

import com.kospot.domain.game.repository.RoadViewGameRepository;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.game.vo.GameStatus;
import com.kospot.domain.game.vo.GameType;
import com.kospot.domain.gamerank.adaptor.GameRankAdaptor;
import com.kospot.domain.gamerank.entity.GameRank;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.memberitem.repository.MemberItemRepository;
import com.kospot.domain.multi.gamePlayer.repository.GamePlayerRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMemberProfileUseCase {

    private final RoadViewGameRepository roadViewGameRepository;
    private final GamePlayerRepository gamePlayerRepository;
    private final MemberItemRepository memberItemRepository;
    private final GameRankAdaptor gameRankAdaptor;

    public MemberProfileResponse execute(Member member) {
        Long memberId = member.getId();
        
        GameStatistics statistics = buildGameStatistics(memberId);
        RankInfo rankInfo = buildRankInfo(member);
        ItemInfo itemInfo = buildItemInfo(member);
        int currentStreak = calculateCurrentStreak(memberId);
        LocalDateTime lastPlayedAt = getLastPlayedAt(memberId);

        return MemberProfileResponse.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .currentPoint(member.getPoint())
                .joinedAt(member.getCreatedDate())
                .lastPlayedAt(lastPlayedAt)
                .currentStreak(currentStreak)
                .statistics(statistics)
                .rankInfo(rankInfo)
                .itemInfo(itemInfo)
                .build();
    }

    private GameStatistics buildGameStatistics(Long memberId) {
        long practiceGames = roadViewGameRepository.countByMemberIdAndGameTypeAndGameStatus(
                memberId, GameType.PRACTICE, GameStatus.COMPLETED);
        double practiceAvgScore = roadViewGameRepository.findAverageScoreByMemberIdAndGameType(
                memberId, GameType.PRACTICE, GameStatus.COMPLETED);

        long rankGames = roadViewGameRepository.countByMemberIdAndGameTypeAndGameStatus(
                memberId, GameType.RANK, GameStatus.COMPLETED);
        double rankAvgScore = roadViewGameRepository.findAverageScoreByMemberIdAndGameType(
                memberId, GameType.RANK, GameStatus.COMPLETED);

        long multiGames = gamePlayerRepository.countByMemberId(memberId);
        double multiAvgScore = gamePlayerRepository.findAverageScoreByMemberId(memberId);
        long firstPlaceCount = gamePlayerRepository.countByMemberIdAndRank(memberId, 1);
        long secondPlaceCount = gamePlayerRepository.countByMemberIdAndRank(memberId, 2);
        long thirdPlaceCount = gamePlayerRepository.countByMemberIdAndRank(memberId, 3);

        double bestScore = roadViewGameRepository.findMaxScoreByMemberId(memberId, GameStatus.COMPLETED);

        return GameStatistics.builder()
                .singleGame(SingleGameStats.builder()
                        .practice(GameModeStats.builder()
                                .totalGames(practiceGames)
                                .averageScore(practiceAvgScore)
                                .build())
                        .rank(GameModeStats.builder()
                                .totalGames(rankGames)
                                .averageScore(rankAvgScore)
                                .build())
                        .build())
                .multiGame(MultiGameStats.builder()
                        .totalGames(multiGames)
                        .averageScore(multiAvgScore)
                        .firstPlaceCount(firstPlaceCount)
                        .secondPlaceCount(secondPlaceCount)
                        .thirdPlaceCount(thirdPlaceCount)
                        .build())
                .bestScore(bestScore)
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

    private int calculateCurrentStreak(Long memberId) {
        List<LocalDateTime> singleGameDates = roadViewGameRepository.findAllCreatedDatesByMemberId(memberId);
        List<LocalDateTime> multiGameDates = gamePlayerRepository.findAllCreatedDatesByMemberId(memberId);

        Set<LocalDate> allPlayDates = new TreeSet<>();
        singleGameDates.forEach(dateTime -> allPlayDates.add(dateTime.toLocalDate()));
        multiGameDates.forEach(dateTime -> allPlayDates.add(dateTime.toLocalDate()));

        if (allPlayDates.isEmpty()) {
            return 0;
        }

        List<LocalDate> sortedDates = new ArrayList<>(allPlayDates);
        sortedDates.sort((a, b) -> b.compareTo(a));

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate latestPlayDate = sortedDates.get(0);

        if (!latestPlayDate.equals(today) && !latestPlayDate.equals(yesterday)) {
            return 0;
        }

        int streak = 0;
        LocalDate expectedDate = latestPlayDate.equals(today) ? today : yesterday;

        for (LocalDate playDate : sortedDates) {
            if (playDate.equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else if (playDate.isBefore(expectedDate)) {
                break;
            }
        }

        return streak;
    }

    private LocalDateTime getLastPlayedAt(Long memberId) {
        LocalDateTime singleGameLastPlayed = roadViewGameRepository.findLatestPlayDateByMemberId(
                memberId, GameStatus.COMPLETED);
        LocalDateTime multiGameLastPlayed = gamePlayerRepository.findLatestPlayDateByMemberId(memberId);

        if (singleGameLastPlayed == null && multiGameLastPlayed == null) {
            return null;
        }
        if (singleGameLastPlayed == null) {
            return multiGameLastPlayed;
        }
        if (multiGameLastPlayed == null) {
            return singleGameLastPlayed;
        }

        return singleGameLastPlayed.isAfter(multiGameLastPlayed) ? singleGameLastPlayed : multiGameLastPlayed;
    }
}

