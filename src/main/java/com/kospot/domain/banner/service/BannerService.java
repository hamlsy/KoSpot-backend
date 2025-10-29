package com.kospot.domain.banner.service;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.domain.image.entity.Image;
import com.kospot.domain.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BannerService {

    private final BannerAdaptor bannerAdaptor;
    private final ImageService imageService;

    @Transactional
    public Banner createBanner(String title, MultipartFile imageFile, String linkUrl, String description, Integer displayOrder) {
        Image image = imageService.uploadBannerImage(imageFile);
        
        Banner banner = Banner.builder()
                .title(title)
                .image(image)
                .linkUrl(linkUrl)
                .description(description)
                .displayOrder(displayOrder)
                .isActive(true)
                .build();
        return bannerAdaptor.save(banner);
    }

    public Banner getBanner(Long id) {
        return bannerAdaptor.findById(id);
    }

    public List<Banner> getAllBanners() {
        return bannerAdaptor.findAll();
    }

    public List<Banner> getActiveBanners() {
        return bannerAdaptor.findAllActive();
    }

    @Transactional
    public void updateBanner(Long id, String title, MultipartFile newImageFile, String linkUrl, String description, Integer displayOrder) {
        Banner banner = bannerAdaptor.findById(id);
        
        Image newImage = null;
        if (newImageFile != null && !newImageFile.isEmpty()) {
            // 기존 이미지 삭제
            if (banner.getImage() != null) {
                imageService.deleteBannerImage(banner.getImage());
            }
            // 새 이미지 업로드
            newImage = imageService.uploadBannerImage(newImageFile);
        }
        
        banner.update(title, newImage, linkUrl, description, displayOrder);
    }

    @Transactional
    public void activateBanner(Long id) {
        Banner banner = bannerAdaptor.findById(id);
        banner.activate();
    }

    @Transactional
    public void deactivateBanner(Long id) {
        Banner banner = bannerAdaptor.findById(id);
        banner.deactivate();
    }

    @Transactional
    public void deleteBanner(Long id) {
        Banner banner = bannerAdaptor.findById(id);
        
        // S3에서 이미지 삭제
        if (banner.getImage() != null) {
            imageService.deleteBannerImage(banner.getImage());
        }
        
        bannerAdaptor.delete(banner);
    }
}

