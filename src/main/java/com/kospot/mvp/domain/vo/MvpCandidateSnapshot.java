package com.kospot.mvp.domain.vo;

import com.kospot.game.domain.entity.RoadViewGame;
import com.kospot.gamerank.domain.entity.GameRank;
import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.mvp.domain.entity.DailyMvp;

import java.time.LocalDateTime;
import java.util.Objects;

public record MvpCandidateSnapshot(
        Long memberId,
        Long roadViewGameId,
        double score,
        LocalDateTime endedAt,
        RankTier rankTier,
        RankLevel rankLevel,
        int ratingScore
) {

    public static MvpCandidateSnapshot from(RoadViewGame game, GameRank gameRank) {
        return new MvpCandidateSnapshot(
                game.getMember().getId(),
                game.getId(),
                game.getScore(),
                game.getEndedAt(),
                gameRank.getRankTier(),
                gameRank.getRankLevel(),
                gameRank.getRatingScore()
        );
    }

    public static MvpCandidateSnapshot from(DailyMvp dailyMvp, LocalDateTime endedAt) {
        return new MvpCandidateSnapshot(
                dailyMvp.getMemberId(),
                dailyMvp.getRoadViewGameId(),
                dailyMvp.getGameScore(),
                endedAt,
                dailyMvp.getRankTier(),
                dailyMvp.getRankLevel(),
                dailyMvp.getRatingScore()
        );
    }

    public void validate() {
        Objects.requireNonNull(memberId, "memberId must not be null");
        Objects.requireNonNull(roadViewGameId, "roadViewGameId must not be null");
        Objects.requireNonNull(endedAt, "endedAt must not be null");
        Objects.requireNonNull(rankTier, "rankTier must not be null");
        Objects.requireNonNull(rankLevel, "rankLevel must not be null");
    }
}
