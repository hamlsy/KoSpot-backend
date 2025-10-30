package com.kospot.presentation.admin.dto.response;

import com.kospot.domain.banner.entity.Banner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AdminBannerResponse {

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
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static BannerInfo from(Banner banner) {
            return BannerInfo.builder()
                    .bannerId(banner.getId())
                    .title(banner.getTitle())
                    .imageUrl(banner.getImageUrl()) // Banner의 getImageUrl() 메서드 사용
                    .linkUrl(banner.getLinkUrl())
                    .description(banner.getDescription())
                    .displayOrder(banner.getDisplayOrder())
                    .isActive(banner.getIsActive())
                    .createdAt(banner.getCreatedDate())
                    .updatedAt(banner.getLastModifiedDate())
                    .build();
        }
    }
}

