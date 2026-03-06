package com.kospot.application.admin.banner;

import com.kospot.banner.application.adaptor.BannerAdaptor;
import com.kospot.member.application.adaptor.MemberAdaptor;
import com.kospot.member.domain.entity.Member;
import com.kospot.infrastructure.annotation.usecase.UseCase;
import com.kospot.presentation.admin.dto.response.AdminBannerResponse;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@UseCase
@RequiredArgsConstructor
public class FindAllBannersUseCase {

    private final MemberAdaptor memberAdaptor;
    private final BannerAdaptor bannerAdaptor;

    public List<AdminBannerResponse.BannerInfo> execute(Long adminId) {
        Member admin = memberAdaptor.queryById(adminId);
        admin.validateAdmin();

        return bannerAdaptor.queryAllFetchImage()
                .stream()
                .map(AdminBannerResponse.BannerInfo::from)
                .collect(Collectors.toList());
    }
}

