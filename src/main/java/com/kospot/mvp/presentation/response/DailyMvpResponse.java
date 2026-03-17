package com.kospot.mvp.presentation.response;

import com.kospot.gamerank.domain.vo.RankLevel;
import com.kospot.gamerank.domain.vo.RankTier;
import com.kospot.mvp.domain.entity.DailyMvp;
import com.kospot.mvp.domain.vo.MvpCandidateSnapshot;
import com.kospot.member.infrastructure.redis.adaptor.MemberProfileRedisAdaptor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

public class DailyMvpResponse {

    @Getter
    @Builder
    public static class DailyWithYesterday {
        private Daily today;
        private Daily yesterday;
    }

    @Getter
    @Builder
    public static class Daily {
        private LocalDate mvpDate;
        private Long memberId;
        private String nickname;
        private String equippedMarkerImageUrl;
        private RankTier rankTier;
        private RankLevel rankLevel;
        private int ratingScore;
        private double gameScore;
        private double answerTime;
        private String poiName;

        public static Daily from(
                DailyMvp dailyMvp,
                MemberProfileRedisAdaptor.MemberProfileView profileView,
                double answerTime
        ) {
            return Daily.builder()
                    .mvpDate(dailyMvp.getMvpDate())
                    .memberId(dailyMvp.getMemberId())
                    .nickname(profileView.nickname())
                    .equippedMarkerImageUrl(profileView.markerImageUrl())
                    .rankTier(dailyMvp.getRankTier())
                    .rankLevel(dailyMvp.getRankLevel())
                    .ratingScore(dailyMvp.getRatingScore())
                    .gameScore(dailyMvp.getGameScore())
                    .answerTime(answerTime)
                    .poiName(dailyMvp.getPoiName())
                    .build();
        }

        public static Daily from(
                LocalDate date,
                MvpCandidateSnapshot snapshot,
                MemberProfileRedisAdaptor.MemberProfileView profileView,
                double answerTime
        ) {
            return Daily.builder()
                    .mvpDate(date)
                    .memberId(snapshot.memberId())
                    .nickname(profileView.nickname())
                    .equippedMarkerImageUrl(profileView.markerImageUrl())
                    .rankTier(snapshot.rankTier())
                    .rankLevel(snapshot.rankLevel())
                    .ratingScore(snapshot.ratingScore())
                    .gameScore(snapshot.score())
                    .answerTime(answerTime)
                    .poiName(snapshot.poiName())
                    .build();
        }
    }
}
