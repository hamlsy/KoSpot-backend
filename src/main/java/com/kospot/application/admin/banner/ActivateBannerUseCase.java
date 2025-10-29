package com.kospot.application.admin.banner;

import com.kospot.domain.banner.service.BannerService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class ActivateBannerUseCase {

    private final BannerService bannerService;

    @Transactional
    public void execute(Member admin, Long bannerId) {
        admin.validateAdmin();
        bannerService.activateBanner(bannerId);
    }
}

