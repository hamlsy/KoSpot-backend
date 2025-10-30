package com.kospot.application.banner;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.banner.dto.response.BannerResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindActiveBannersUseCase {

    private final BannerAdaptor bannerAdaptor;

    public List<BannerResponse.BannerInfo> execute() {
        return bannerAdaptor.queryAllActive()
                .stream()
                .map(BannerResponse.BannerInfo::from)
                .collect(Collectors.toList());
    }
}

