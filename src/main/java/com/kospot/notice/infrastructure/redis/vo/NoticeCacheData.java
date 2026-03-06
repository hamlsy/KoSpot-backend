package com.kospot.notice.infrastructure.redis.vo;

import com.kospot.notice.domain.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Redis 캐시용 Notice 데이터 VO
 * 최근 공지사항의 메인 페이지 표시에 필요한 최소 정보만 포함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeCacheData {

    private Long noticeId;
    private String title;
    private LocalDateTime createdDate;

    public static NoticeCacheData from(Notice notice) {
        return NoticeCacheData.builder()
                .noticeId(notice.getId())
                .title(notice.getTitle())
                .createdDate(notice.getCreatedDate())
                .build();
    }
}
