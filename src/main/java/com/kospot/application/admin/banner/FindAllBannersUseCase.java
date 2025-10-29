package com.kospot.application.admin.banner;

import com.kospot.domain.banner.service.BannerService;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.AdminBannerResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindAllBannersUseCase {

    private final BannerService bannerService;

    public List<AdminBannerResponse.BannerInfo> execute(Member admin) {
        admin.validateAdmin();

        return bannerService.getAllBanners()
                .stream()
                .map(AdminBannerResponse.BannerInfo::from)
                .collect(Collectors.toList());
    }
}

