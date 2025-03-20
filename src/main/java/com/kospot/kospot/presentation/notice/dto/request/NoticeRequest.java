package com.kospot.kospot.presentation.notice.dto.request;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class NoticeRequest {

    @Data
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {

        private String title;
        private String content;
        private List<MultipartFile> images;
    }

    @Data
    @Builder
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {

        private String title;
        private String content;

    }

}
