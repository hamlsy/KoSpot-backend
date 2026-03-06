package com.kospot.infrastructure.redis.domain.notice.service;

import com.kospot.notice.application.adaptor.NoticeAdaptor;
import com.kospot.notice.domain.entity.Notice;
import com.kospot.infrastructure.redis.domain.notice.dao.NoticeCacheRedisRepository;
import com.kospot.infrastructure.redis.domain.notice.vo.NoticeCacheData;
import com.kospot.notice.presentation.dto.response.NoticeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class RecentNoticeCacheService {

    private final NoticeCacheRedisRepository noticeCacheRedisRepository;
    private final NoticeAdaptor noticeAdaptor;

    private static final int RECENT_NOTICE_LIMIT = 3;

    /**
     * 최근 공지사항 목록 조회 (Cache-Aside 패턴)
     * 1. Redis 캐시 조회
     * 2. 캐시 미스 시 DB 조회
     * 3. DB 결과를 캐시에 저장
     * 4. NoticeResponse.Summary 목록 반환
     */
    public List<NoticeResponse.Summary> getRecentNotices() {
        // 1. 캐시 조회
        Optional<List<NoticeCacheData>> cachedNotices = noticeCacheRedisRepository.findAll();

        if (cachedNotices.isPresent()) {
            log.debug("Cache HIT: Recent notices loaded from Redis");
            return cachedNotices.get().stream()
                    .map(this::toResponse)
                    .collect(Collectors.toList());
        }

        // 2. 캐시 미스 - DB 조회
        List<Notice> recentNotices = noticeAdaptor.queryRecentNotices(RECENT_NOTICE_LIMIT);

        // 3. 캐시 저장
        List<NoticeCacheData> cacheDataList = recentNotices.stream()
                .map(NoticeCacheData::from)
                .collect(Collectors.toList());
        noticeCacheRedisRepository.saveAll(cacheDataList);

        // 4. Response 변환 및 반환
        return recentNotices.stream()
                .map(NoticeResponse.Summary::from)
                .collect(Collectors.toList());
    }

    /**
     * 캐시 무효화
     */
    public void evictCache() {
        noticeCacheRedisRepository.deleteAll();
    }

    /**
     * NoticeCacheData를 NoticeResponse.Summary로 변환
     */
    private NoticeResponse.Summary toResponse(NoticeCacheData cacheData) {
        return NoticeResponse.Summary.builder()
                .noticeId(cacheData.getNoticeId())
                .title(cacheData.getTitle())
                .createdDate(cacheData.getCreatedDate())
                .build();
    }
}
