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

    public Banner findById(Long id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new BannerHandler(ErrorStatus.BANNER_NOT_FOUND));
    }

    public List<Banner> findAll() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc();
    }

    public List<Banner> findAllActive() {
        return bannerRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    public void delete(Banner banner) {
        bannerRepository.delete(banner);
    }
}

