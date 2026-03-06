package com.kospot.admin.application.usecase.banner;

import com.kospot.banner.application.adaptor.BannerAdaptor;
import com.kospot.banner.domain.entity.Banner;
import com.kospot.banner.application.service.BannerService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.common.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.banner.service.ActiveBannerCacheService;
import com.kospot.admin.presentation.dto.request.AdminBannerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class UpdateBannerUseCase {

    private final MemberAdaptor memberAdaptor;
    private final BannerAdaptor bannerAdaptor;
    private final BannerService bannerService;
    private final ActiveBannerCacheService activeBannerCacheService;

    @Transactional
    public void execute(Long adminId, Long bannerId, AdminBannerRequest.Update request) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        Banner banner = bannerAdaptor.queryById(bannerId);

        bannerService.updateBanner(
                banner,
                request.getTitle(),
                request.getImage(),
                request.getLinkUrl(),
                request.getDescription(),
                request.getDisplayOrder());

        // 캐시 무효화
        activeBannerCacheService.evictCache();
    }
}
