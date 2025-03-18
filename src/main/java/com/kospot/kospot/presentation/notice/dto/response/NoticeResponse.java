package com.kospot.kospot.presentation.notice.dto.response;

import com.kospot.kospot.domain.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;


public class NoticeResponse {

    @Getter
    @Builder
    @ToString
    public static class Summary {
        private Long noticeId;
        private String title;
        private LocalDateTime createdDate;

        public static Summary from(Notice notice) {
            return Summary.builder()
                    .noticeId(notice.getId())
                    .title(notice.getTitle())
                    .createdDate(notice.getCreatedDate())
                    .build();
        }

    }

}
