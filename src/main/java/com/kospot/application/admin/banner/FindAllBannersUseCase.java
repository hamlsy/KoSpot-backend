package com.kospot.application.admin.banner;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.domain.member.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.AdminBannerResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindAllBannersUseCase {

    private final BannerAdaptor bannerAdaptor;

    public List<AdminBannerResponse.BannerInfo> execute(Member admin) {
        admin.validateAdmin();

        return bannerAdaptor.queryAllFetchImage()
                .stream()
                .map(AdminBannerResponse.BannerInfo::from)
                .collect(Collectors.toList());
    }
}

