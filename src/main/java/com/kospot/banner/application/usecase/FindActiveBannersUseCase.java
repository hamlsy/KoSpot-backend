package com.kospot.banner.application.usecase;

import com.kospot.banner.application.adaptor.BannerAdaptor;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.banner.presentation.response.BannerResponse;
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

