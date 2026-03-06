package com.kospot.application.admin.banner;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.banner.service.BannerService;
import com.kospot.domain.member.adaptor.MemberAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.banner.service.ActiveBannerCacheService;
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
