package com.kospot.admin.application.usecase.banner;

import com.kospot.banner.application.adaptor.BannerAdaptor;
import com.kospot.banner.domain.entity.Banner;
import com.kospot.banner.application.service.BannerService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.banner.infrastructure.redis.service.ActiveBannerCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteBannerUseCase {

    private final MemberAdaptor memberAdaptor;
    private final BannerAdaptor bannerAdaptor;
    private final BannerService bannerService;
    private final ActiveBannerCacheService activeBannerCacheService;

    @Transactional
    public void execute(Long adminId, Long bannerId) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();
        Banner banner = bannerAdaptor.queryById(bannerId);
        bannerService.deleteBanner(banner);

        // 캐시 무효화
        activeBannerCacheService.evictCache();
    }
}
