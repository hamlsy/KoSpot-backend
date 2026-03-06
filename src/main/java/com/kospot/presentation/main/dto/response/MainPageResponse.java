package com.kospot.presentation.main.dto.response;

import com.kospot.member.domain.entity.Member;
import com.kospot.statistic.domain.entity.MemberStatistic;
import com.kospot.banner.presentation.response.BannerResponse;
import com.kospot.notice.presentation.dto.response.NoticeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class MainPageResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MainPageInfo {
        private MyInfo myInfo;
        private List<NoticeResponse.Summary> recentNotices;
        private List<BannerResponse.BannerInfo> banners;

        public static MainPageInfo of(
                MyInfo myInfo,
                List<NoticeResponse.Summary> recentNotices,
                List<BannerResponse.BannerInfo> banners
        ) {
            return MainPageInfo.builder()
                    .myInfo(myInfo)
                    .recentNotices(recentNotices)
                    .banners(banners)
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MyInfo {
        private String nickname;
        private String email;
        private String equippedMarkerImageUrl;
        private Boolean isAdmin;
        private Boolean isFirstVisited;
        private LocalDateTime lastPlayedAt;
        private int currentPoint;

        public static MyInfo of(
                Member member, MemberStatistic statistic, String equippedMarkerImageUrl
        ) {
            return MyInfo.builder()
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .equippedMarkerImageUrl(equippedMarkerImageUrl)
                    .isAdmin(member.isAdmin())
                    .isFirstVisited(member.isFirstVisited())
                    .lastPlayedAt(statistic.getLastPlayedAt())
                    .currentPoint(member.getPoint())
                    .build();
        }
    }

}

