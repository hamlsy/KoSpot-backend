package com.kospot.presentation.main.dto.response;

import com.kospot.domain.member.entity.Member;
import com.kospot.presentation.banner.dto.response.BannerResponse;
import com.kospot.presentation.notice.dto.response.NoticeResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class MainPageResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MainPageInfo {
        private MyInfo myInfo;
        private GameModeStatus gameModeStatus;
        private List<NoticeResponse.Summary> recentNotices;
        private List<BannerResponse.BannerInfo> banners;

        public static MainPageInfo of(
                MyInfo myInfo,
                GameModeStatus gameModeStatus,
                List<NoticeResponse.Summary> recentNotices,
                List<BannerResponse.BannerInfo> banners
        ) {
            return MainPageInfo.builder()
                    .myInfo(myInfo)
                    .gameModeStatus(gameModeStatus)
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
        public static MyInfo of(
                Member member
        ) {
            return MyInfo.builder()
                    .nickname(member.getNickname())
                    .email(member.getEmail())
                    .equippedMarkerImageUrl(member.getEquippedMarkerImage().getImageUrl())
                    .isAdmin(member.isAdmin())
                    .isFirstVisited(member.isFirstVisited())
                    .build();
        }
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GameModeStatus {
        private Boolean roadviewEnabled;
        private Boolean photoEnabled;
        private Boolean multiplayEnabled;

        public static GameModeStatus of(
                Boolean roadviewEnabled,
                Boolean photoEnabled,
                Boolean multiplayEnabled
        ) {
            return GameModeStatus.builder()
                    .roadviewEnabled(roadviewEnabled)
                    .photoEnabled(photoEnabled)
                    .multiplayEnabled(multiplayEnabled)
                    .build();
        }
    }
}

