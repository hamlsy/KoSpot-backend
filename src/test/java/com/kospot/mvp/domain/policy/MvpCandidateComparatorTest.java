package com.kospot.mvp.domain.policy;

import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MvpCandidateComparatorTest {

    private final MvpCandidateComparator comparator = new MvpCandidateComparator();

    @Test
    @DisplayName("점수가 높으면 더 우수한 후보로 판단한다")
    void higherScoreWins() {
        MvpCandidateSnapshot current = snapshot(100L, 900.0, LocalDateTime.of(2026, 3, 10, 10, 0));
        MvpCandidateSnapshot candidate = snapshot(101L, 950.0, LocalDateTime.of(2026, 3, 10, 10, 1));

        assertTrue(comparator.isBetter(candidate, current));
    }

    @Test
    @DisplayName("점수가 같으면 먼저 종료된 게임이 우선한다")
    void earlierEndedAtWinsOnTieScore() {
        MvpCandidateSnapshot current = snapshot(100L, 900.0, LocalDateTime.of(2026, 3, 10, 10, 5));
        MvpCandidateSnapshot candidate = snapshot(101L, 900.0, LocalDateTime.of(2026, 3, 10, 10, 1));

        assertTrue(comparator.isBetter(candidate, current));
    }

    @Test
    @DisplayName("점수와 종료 시간이 같으면 게임 ID가 작은 쪽이 우선한다")
    void smallerGameIdWinsOnFullTie() {
        LocalDateTime endedAt = LocalDateTime.of(2026, 3, 10, 10, 5);
        MvpCandidateSnapshot current = snapshot(100L, 900.0, endedAt);
        MvpCandidateSnapshot candidate = snapshot(99L, 900.0, endedAt);

        assertTrue(comparator.isBetter(candidate, current));
        assertFalse(comparator.isBetter(current, candidate));
    }

    private MvpCandidateSnapshot snapshot(Long gameId, double score, LocalDateTime endedAt) {
        return new MvpCandidateSnapshot(1L, gameId, "poi", score, endedAt, RankTier.GOLD, RankLevel.ONE, 2300);
    }
}
