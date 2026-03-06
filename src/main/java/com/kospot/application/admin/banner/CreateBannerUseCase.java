package com.kospot.application.admin.banner;

import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.banner.service.BannerService;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.infrastructure.redis.domain.banner.service.ActiveBannerCacheService;
import com.kospot.presentation.admin.dto.request.AdminBannerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateBannerUseCase {

    private final MemberAdaptor memberAdaptor;
    private final BannerService bannerService;
    private final ActiveBannerCacheService activeBannerCacheService;

    @Transactional
    public Long execute(Long adminId, AdminBannerRequest.Create request) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        Banner banner = bannerService.createBanner(
                request.getTitle(),
                request.getImage(),
                request.getLinkUrl(),
                request.getDescription(),
                request.getDisplayOrder()
        );

        // 캐시 무효화
        activeBannerCacheService.evictCache();

        return banner.getId();
    }
}

