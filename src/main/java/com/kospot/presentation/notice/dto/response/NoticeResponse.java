package com.kospot.presentation.notice.dto.response;

import com.kospot.domain.notice.entity.Notice;
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

    @Getter
    @Builder
    @ToString
    public static class Detail {
        private Long noticeId;
        private String title;
        private String content;
        private LocalDateTime createdDate;
        public static Detail from(Notice notice) {
            return Detail.builder()
                    .noticeId(notice.getId())
                    .title(notice.getTitle())
                    .content(notice.getContent())
                    .createdDate(notice.getCreatedDate())
                    .build();
        }

    }

}
