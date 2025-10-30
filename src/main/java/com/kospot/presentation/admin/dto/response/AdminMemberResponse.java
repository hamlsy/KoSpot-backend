package com.kospot.presentation.admin.dto.response;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.member.entity.MemberStatistic;
import com.kospot.domain.member.vo.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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

        // 통계 정보
        private Long singlePracticeGames;
        private Double singlePracticeAvgScore;
        private Long singleRankGames;
        private Double singleRankAvgScore;
        private Long multiGames;
        private Double multiAvgScore;
        private Long multiFirstPlace;
        private Long multiSecondPlace;
        private Long multiThirdPlace;
        private Double bestScore;
        private Integer currentStreak;
        private Integer longestStreak;

        public static MemberDetail of(Member member, MemberStatistic statistic) {
            return MemberDetail.builder()
                    .memberId(member.getId())
                    .username(member.getUsername())
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .role(member.getRole())
                    .point(member.getPoint())
                    .createdAt(member.getCreatedDate())
                    .updatedAt(member.getLastModifiedDate())
                    .singlePracticeGames(statistic.getSinglePracticeGames())
                    .singlePracticeAvgScore(statistic.getSinglePracticeAvgScore())
                    .singleRankGames(statistic.getSingleRankGames())
                    .singleRankAvgScore(statistic.getSingleRankAvgScore())
                    .multiGames(statistic.getMultiGames())
                    .multiAvgScore(statistic.getMultiAvgScore())
                    .multiFirstPlace(statistic.getMultiFirstPlace())
                    .multiSecondPlace(statistic.getMultiSecondPlace())
                    .multiThirdPlace(statistic.getMultiThirdPlace())
                    .bestScore(statistic.getBestScore())
                    .currentStreak(statistic.getCurrentStreak())
                    .longestStreak(statistic.getLongestStreak())
                    .build();
        }
    }
}

