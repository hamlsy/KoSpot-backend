package com.kospot.application.main;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.member.entity.Member;
import com.kospot.domain.notice.adaptor.NoticeAdaptor;
import com.kospot.domain.notice.entity.Notice;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.statistic.entity.MemberStatistic;
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

    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final NoticeAdaptor noticeAdaptor;
    private final BannerAdaptor bannerAdaptor;

    private static final int RECENT_NOTICE_LIMIT = 3;

    public MainPageResponse.MainPageInfo execute(Member member) {
        // 관리자 여부 확인
        MainPageResponse.MyInfo myInfo = null;
        if(member != null) {
            MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
            myInfo = MainPageResponse.MyInfo.of(member, statistic);
        }

        // 최근 공지사항 3개 조회 todo redis
        List<Notice> recentNotices = noticeAdaptor.queryRecentNotices(RECENT_NOTICE_LIMIT);
        List<NoticeResponse.Summary> noticeSummaries = recentNotices.stream()
                .map(NoticeResponse.Summary::from)
                .collect(Collectors.toList());

        // 활성화된 배너 조회 todo redis
        List<Banner> activeBanners = bannerAdaptor.queryAllActive();
        List<BannerResponse.BannerInfo> banners = activeBanners.stream()
                .map(BannerResponse.BannerInfo::from)
                .collect(Collectors.toList());

        return MainPageResponse.MainPageInfo.of(
                myInfo, noticeSummaries, banners);
    }
}

