package com.kospot.presentation.admin.dto.response;

import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.vo.Role;
import com.kospot.domain.statistic.entity.GameModeStatistic;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.domain.statistic.vo.PlayStreak;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class AdminMemberResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MemberInfo {
        private Long memberId;
        private String username;
        private String nickname;
        private String email;
        private Role role;
        private Integer point;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static MemberInfo from(Member member) {
            return MemberInfo.builder()
                    .memberId(member.getId())
                    .username(member.getUsername())
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .role(member.getRole())
                    .point(member.getPoint())
                    .createdAt(member.getCreatedDate())
                    .updatedAt(member.getLastModifiedDate())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MemberDetail {
        private Long memberId;
        private String username;
        private String nickname;
        private String email;
        private Role role;
        private Integer point;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // 통계 정보 (로드뷰 모드)
        private Long roadviewPracticeGames;
        private Double roadviewPracticeAvgScore;
        private Long roadviewRankGames;
        private Double roadviewRankAvgScore;
        private Long roadviewMultiGames;
        private Double roadviewMultiAvgScore;
        private Long roadviewMultiFirstPlace;
        private Long roadviewMultiSecondPlace;
        private Long roadviewMultiThirdPlace;

        // 통계 정보 (포토 모드)
        private Long photoPracticeGames;
        private Double photoPracticeAvgScore;
        private Long photoRankGames;
        private Double photoRankAvgScore;
        private Long photoMultiGames;
        private Double photoMultiAvgScore;
        private Long photoMultiFirstPlace;
        private Long photoMultiSecondPlace;
        private Long photoMultiThirdPlace;

        // 공통 통계
        private Double bestScore;
        private Integer currentStreak;
        private Integer longestStreak;

        public static MemberDetail of(Member member, MemberStatistic statistic) {
            GameModeStatistic roadViewStatistic = findModeStatistic(statistic, GameMode.ROADVIEW);
            GameModeStatistic photoStatistic = findModeStatistic(statistic, GameMode.PHOTO);
            PlayStreak playStreak = statistic.getPlayStreak();

            return MemberDetail.builder()
                    .memberId(member.getId())
                    .username(member.getUsername())
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .role(member.getRole())
                    .point(member.getPoint())
                    .createdAt(member.getCreatedDate())
                    .updatedAt(member.getLastModifiedDate())
                    // 로드뷰
                    .roadviewPracticeGames(roadViewStatistic.getPractice().getGames())
                    .roadviewPracticeAvgScore(roadViewStatistic.getPractice().getAvgScore())
                    .roadviewRankGames(roadViewStatistic.getRank().getGames())
                    .roadviewRankAvgScore(roadViewStatistic.getRank().getAvgScore())
                    .roadviewMultiGames(roadViewStatistic.getMulti().getGames())
                    .roadviewMultiAvgScore(roadViewStatistic.getMulti().getAvgScore())
                    .roadviewMultiFirstPlace(roadViewStatistic.getMulti().getFirstPlace())
                    .roadviewMultiSecondPlace(roadViewStatistic.getMulti().getSecondPlace())
                    .roadviewMultiThirdPlace(roadViewStatistic.getMulti().getThirdPlace())
                    // 포토
                    .photoPracticeGames(photoStatistic.getPractice().getGames())
                    .photoPracticeAvgScore(photoStatistic.getPractice().getAvgScore())
                    .photoRankGames(photoStatistic.getRank().getGames())
                    .photoRankAvgScore(photoStatistic.getRank().getAvgScore())
                    .photoMultiGames(photoStatistic.getMulti().getGames())
                    .photoMultiAvgScore(photoStatistic.getMulti().getAvgScore())
                    .photoMultiFirstPlace(photoStatistic.getMulti().getFirstPlace())
                    .photoMultiSecondPlace(photoStatistic.getMulti().getSecondPlace())
                    .photoMultiThirdPlace(photoStatistic.getMulti().getThirdPlace())
                    // 공통
                    .bestScore(calculateBestScore(statistic.getModeStatistics()))
                    .currentStreak(playStreak != null ? playStreak.getCurrentStreak() : 0)
                    .longestStreak(playStreak != null ? playStreak.getLongestStreak() : 0)
                    .build();
        }

        private static GameModeStatistic findModeStatistic(MemberStatistic statistic, GameMode gameMode) {
            return statistic.getModeStatistics().stream()
                    .filter(modeStatistic -> modeStatistic.getGameMode() == gameMode)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("GameModeStatistic not found: " + gameMode));
        }

        private static double calculateBestScore(List<GameModeStatistic> modeStatistics) {
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
    }
}
