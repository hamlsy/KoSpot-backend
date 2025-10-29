package com.kospot.application.admin.banner;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.banner.service.BannerService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteBannerUseCase {

    private final BannerAdaptor bannerAdaptor;
    private final BannerService bannerService;

    @Transactional
    public void execute(Member admin, Long bannerId) {
        admin.validateAdmin();
        Banner banner = bannerAdaptor.queryById(bannerId);
        bannerService.deleteBanner(banner);
    }
}

