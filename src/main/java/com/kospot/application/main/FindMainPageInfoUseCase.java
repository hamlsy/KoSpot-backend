package com.kospot.application.main;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.game.vo.GameMode;
import com.kospot.domain.gameconfig.adaptor.GameConfigAdaptor;
import com.kospot.domain.gameconfig.entity.GameConfig;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.banner.dto.response.BannerResponse;
import com.kospot.presentation.main.dto.response.MainPageResponse;
import com.kospot.presentation.notice.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindMainPageInfoUseCase {

    private final GameConfigAdaptor gameConfigAdaptor;
    private final NoticeAdaptor noticeAdaptor;
    private final BannerAdaptor bannerAdaptor;

    private static final int RECENT_NOTICE_LIMIT = 3;

    public MainPageResponse.MainPageInfo execute(Member member) {
        // 관리자 여부 확인
        Boolean isAdmin = member.isAdmin();

        // 활성화된 게임 모드 조회
        List<GameConfig> activeGameConfigs = gameConfigAdaptor.queryAllActive();

        // GameConfig가 하나도 없으면 기본값으로 모두 활성화 (true)
        Boolean roadviewEnabled;
        Boolean photoEnabled;
        Boolean multiplayEnabled;

        if (activeGameConfigs.isEmpty()) {
            // 기본값: 모두 활성화
            roadviewEnabled = true;
            photoEnabled = true;
            multiplayEnabled = true;
        } else {
            // 실제 설정 기반으로 활성화 여부 확인
            roadviewEnabled = activeGameConfigs.stream()
                    .anyMatch(config -> config.getGameMode() == GameMode.ROADVIEW);

            photoEnabled = activeGameConfigs.stream()
                    .anyMatch(config -> config.getGameMode() == GameMode.PHOTO);

            multiplayEnabled = activeGameConfigs.stream()
                    .anyMatch(config -> !config.getIsSingleMode());
        }

        MainPageResponse.GameModeStatus gameModeStatus = MainPageResponse.GameModeStatus.of(
                roadviewEnabled,
                photoEnabled,
                multiplayEnabled
        );

        // 최근 공지사항 3개 조회
        List<Notice> recentNotices = noticeAdaptor.queryRecentNotices(RECENT_NOTICE_LIMIT);
        List<NoticeResponse.Summary> noticeSummaries = recentNotices.stream()
                .map(NoticeResponse.Summary::from)
                .collect(Collectors.toList());

        // 활성화된 배너 조회
        List<Banner> activeBanners = bannerAdaptor.queryAllActive();
        List<BannerResponse.BannerInfo> banners = activeBanners.stream()
                .map(BannerResponse.BannerInfo::from)
                .collect(Collectors.toList());

        return MainPageResponse.MainPageInfo.of(isAdmin, gameModeStatus, noticeSummaries, banners);
    }
}

