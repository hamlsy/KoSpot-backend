package com.kospot.application.banner;

import com.kospot.domain.banner.service.BannerService;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.banner.dto.response.BannerResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindActiveBannersUseCase {

    private final BannerService bannerService;

    public List<BannerResponse.BannerInfo> execute() {
        return bannerService.getActiveBanners()
                .stream()
                .map(BannerResponse.BannerInfo::from)
                .collect(Collectors.toList());
    }
}

