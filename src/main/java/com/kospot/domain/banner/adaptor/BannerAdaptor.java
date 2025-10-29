package com.kospot.domain.banner.adaptor;

import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.banner.repository.BannerRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.exception.object.domain.BannerHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
public class BannerAdaptor {

    private final BannerRepository bannerRepository;

    public Banner save(Banner banner) {
        return bannerRepository.save(banner);
    }

    public Banner queryById(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new BannerHandler(ErrorStatus.BANNER_NOT_FOUND));
    }

    public List<Banner> queryAll() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Banner> queryAllActive() {
        return bannerRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public Banner queryByIdFetchImage(Long id) {
        return bannerRepository.findByIdFetchImage(id)
                .orElseThrow(() -> new BannerHandler(ErrorStatus.BANNER_NOT_FOUND));
    }

    public List<Banner> queryAllFetchImage() {
        return bannerRepository.findAllFetchImage();
    }
}

