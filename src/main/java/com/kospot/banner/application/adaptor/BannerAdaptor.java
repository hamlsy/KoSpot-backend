package com.kospot.banner.application.adaptor;

import com.kospot.banner.domain.entity.Banner;
import com.kospot.banner.infrastructure.persistence.BannerRepository;
import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import com.kospot.infrastructure.exception.object.domain.BannerHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerAdaptor {

    private final BannerRepository bannerRepository;

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

