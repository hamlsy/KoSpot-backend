package com.kospot.presentation.banner.dto.response;

import com.kospot.domain.banner.entity.Banner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class BannerResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class BannerInfo {
        private Long bannerId;
        private String title;
        private String imageUrl;
        private String linkUrl;
        private String description;
        private Integer displayOrder;

        public static BannerInfo from(Banner banner) {
            return BannerInfo.builder()
                    .bannerId(banner.getId())
                    .title(banner.getTitle())
                    .imageUrl(banner.getImageUrl())
                    .linkUrl(banner.getLinkUrl())
                    .description(banner.getDescription())
                    .displayOrder(banner.getDisplayOrder())
                    .build();
        }
    }
}

