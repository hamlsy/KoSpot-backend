package com.kospot.application.main;

import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.domain.statistic.adaptor.MemberStatisticAdaptor;
import com.kospot.domain.statistic.entity.MemberStatistic;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.banner.service.ActiveBannerCacheService;
import com.kospot.infrastructure.redis.domain.member.adaptor.MemberProfileRedisAdaptor;
import com.kospot.infrastructure.redis.domain.notice.service.RecentNoticeCacheService;
import com.kospot.presentation.banner.dto.response.BannerResponse;
import com.kospot.presentation.main.dto.response.MainPageResponse;
import com.kospot.presentation.notice.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FindMainPageInfoUseCase {

    private final MemberAdaptor memberAdaptor;
    private final MemberStatisticAdaptor memberStatisticAdaptor;
    private final ActiveBannerCacheService activeBannerCacheService;
    private final RecentNoticeCacheService recentNoticeCacheService;

    private final MemberProfileRedisAdaptor memberProfileRedisAdaptor;

    public MainPageResponse.MainPageInfo execute(Long memberId) {
        Member member = memberId == null ? null : memberAdaptor.queryById(memberId);
        // 멤버 정보 조회
        MainPageResponse.MyInfo myInfo = null;
        if (member != null) {
            MemberStatistic statistic = memberStatisticAdaptor.queryByMember(member);
            MemberProfileRedisAdaptor.MemberProfileView profileView = memberProfileRedisAdaptor.findProfile(member.getId());
            String profileImageUrl = profileView.markerImageUrl();
            myInfo = MainPageResponse.MyInfo.of(member, statistic, profileImageUrl);
        }

        // 최근 공지사항 3개 조회 (Redis 캐시 우선)
        List<NoticeResponse.Summary> noticeSummaries = recentNoticeCacheService.getRecentNotices();

        // 활성화된 배너 조회 (Redis 캐시 우선)
        List<BannerResponse.BannerInfo> banners = activeBannerCacheService.getActiveBanners();

        return MainPageResponse.MainPageInfo.of(myInfo, noticeSummaries, banners);
    }
}
