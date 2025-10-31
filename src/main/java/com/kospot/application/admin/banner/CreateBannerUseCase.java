package com.kospot.application.admin.banner;

import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.banner.service.BannerService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.request.AdminBannerRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateBannerUseCase {

    private final BannerService bannerService;

    @Transactional
    public Long execute(Member admin, AdminBannerRequest.Create request) {
        admin.validateAdmin();

        Banner banner = bannerService.createBanner(
                request.getTitle(),
                request.getImage(),
                request.getLinkUrl(),
                request.getDescription(),
                request.getDisplayOrder()
        );

        return banner.getId();
    }
}

