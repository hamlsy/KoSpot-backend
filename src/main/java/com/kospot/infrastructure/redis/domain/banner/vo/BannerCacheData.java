package com.kospot.infrastructure.redis.domain.banner.vo;

import com.kospot.domain.banner.entity.Banner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Redis 캐시용 Banner 데이터 VO
 * Active 배너의 메인 페이지 표시에 필요한 최소 정보만 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerCacheData {

    private Long bannerId;
    private String imageUrl;
    private String linkUrl;
    private Integer displayOrder;

    public static BannerCacheData from(Banner banner) {
        return BannerCacheData.builder()
                .bannerId(banner.getId())
                .imageUrl(banner.getImageUrl())
                .linkUrl(banner.getLinkUrl())
                .displayOrder(banner.getDisplayOrder())
                .build();
    }
}
