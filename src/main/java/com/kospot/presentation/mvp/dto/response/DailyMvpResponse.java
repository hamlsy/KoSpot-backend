package com.kospot.presentation.mvp.dto.response;

import com.kospot.domain.gamerank.vo.RankLevel;
import com.kospot.domain.gamerank.vo.RankTier;
import com.kospot.domain.mvp.entity.DailyMvp;
import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

public class DailyMvpResponse {

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
        private String poiName;

        public static Daily from(DailyMvp dailyMvp, MemberProfileRedisAdaptor.MemberProfileView profileView) {
            return Daily.builder()
                    .mvpDate(dailyMvp.getMvpDate())
                    .memberId(dailyMvp.getMemberId())
                    .nickname(profileView.nickname())
                    .equippedMarkerImageUrl(profileView.markerImageUrl())
                    .rankTier(dailyMvp.getRankTier())
                    .rankLevel(dailyMvp.getRankLevel())
                    .ratingScore(dailyMvp.getRatingScore())
                    .gameScore(dailyMvp.getGameScore())
                    .poiName(dailyMvp.getPoiName())
                    .build();
        }
    }
}
