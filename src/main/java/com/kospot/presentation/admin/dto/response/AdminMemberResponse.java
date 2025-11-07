package com.kospot.presentation.admin.dto.response;

import com.kospot.domain.member.entity.Member;
import com.kospot.domain.statistic.entity.MemberStatistic;
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
            return MemberDetail.builder()
                    .memberId(member.getId())
                    .username(member.getUsername())
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .role(member.getRole())
                    .point(member.getPoint())
                    .createdAt(member.getCreatedDate())
                    .updatedAt(member.getLastModifiedDate())
                    .roadviewPracticeGames(statistic.getRoadviewPracticeGames())
                    .roadviewPracticeAvgScore(statistic.getRoadviewPracticeAvgScore())
                    .roadviewRankGames(statistic.getRoadviewRankGames())
                    .roadviewRankAvgScore(statistic.getRoadviewRankAvgScore())
                    .roadviewMultiGames(statistic.getRoadviewMultiGames())
                    .roadviewMultiAvgScore(statistic.getRoadviewMultiAvgScore())
                    .roadviewMultiFirstPlace(statistic.getRoadviewMultiFirstPlace())
                    .roadviewMultiSecondPlace(statistic.getRoadviewMultiSecondPlace())
                    .roadviewMultiThirdPlace(statistic.getRoadviewMultiThirdPlace())
                    .photoPracticeGames(statistic.getPhotoPracticeGames())
                    .photoPracticeAvgScore(statistic.getPhotoPracticeAvgScore())
                    .photoRankGames(statistic.getPhotoRankGames())
                    .photoRankAvgScore(statistic.getPhotoRankAvgScore())
                    .photoMultiGames(statistic.getPhotoMultiGames())
                    .photoMultiAvgScore(statistic.getPhotoMultiAvgScore())
                    .photoMultiFirstPlace(statistic.getPhotoMultiFirstPlace())
                    .photoMultiSecondPlace(statistic.getPhotoMultiSecondPlace())
                    .photoMultiThirdPlace(statistic.getPhotoMultiThirdPlace())
                    .bestScore(statistic.getBestScore())
                    .currentStreak(statistic.getCurrentStreak())
                    .longestStreak(statistic.getLongestStreak())
                    .build();
        }
    }
}

