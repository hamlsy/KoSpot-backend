package com.kospot.infrastructure.redis.domain.banner.service;

import com.kospot.domain.banner.adaptor.BannerAdaptor;
import com.kospot.domain.banner.entity.Banner;
import com.kospot.infrastructure.redis.domain.banner.dao.BannerCacheRedisRepository;
import com.kospot.infrastructure.redis.domain.banner.vo.BannerCacheData;
import com.kospot.presentation.banner.dto.response.BannerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 활성 배너 캐시 서비스
 * Cache-Aside 패턴 구현: Redis 조회 → 없으면 DB 조회 → 캐시 저장 → 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveBannerCacheService {

    private final BannerCacheRedisRepository bannerCacheRedisRepository;
    private final BannerAdaptor bannerAdaptor;

    /**
     * 활성 배너 목록 조회 (Cache-Aside 패턴)
     * 1. Redis 캐시 조회
     * 2. 캐시 미스 시 DB 조회
     * 3. DB 결과를 캐시에 저장
     * 4. BannerResponse.BannerInfo 목록 반환
     */
    public List<BannerResponse.BannerInfo> getActiveBanners() {
        // 1. 캐시 조회
        Optional<List<BannerCacheData>> cachedBanners = bannerCacheRedisRepository.findAll();

        if (cachedBanners.isPresent()) {
            log.debug("Cache HIT: Active banners loaded from Redis");
            return cachedBanners.get().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // 2. 캐시 미스 - DB 조회
        List<Banner> activeBanners = bannerAdaptor.queryAllActive();

        // 3. 캐시 저장
        List<BannerCacheData> cacheDataList = activeBanners.stream()
                .map(BannerCacheData::from)
                .collect(Collectors.toList());
        bannerCacheRedisRepository.saveAll(cacheDataList);

        // 4. Response 변환 및 반환
        return activeBanners.stream()
                .map(BannerResponse.BannerInfo::from)
                .collect(Collectors.toList());
    }

    /**
     * 캐시 무효화
     * Banner CRUD 작업 후 호출되어 캐시를 삭제함
     */
    public void evictCache() {
        bannerCacheRedisRepository.deleteAll();
    }

    /**
     * BannerCacheData를 BannerResponse.BannerInfo로 변환
     */
    private BannerResponse.BannerInfo toResponse(BannerCacheData cacheData) {
        return BannerResponse.BannerInfo.builder()
                .bannerId(cacheData.getBannerId())
                .imageUrl(cacheData.getImageUrl())
                .linkUrl(cacheData.getLinkUrl())
                .displayOrder(cacheData.getDisplayOrder())
                .build();
    }
}
