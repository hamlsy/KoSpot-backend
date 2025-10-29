package com.kospot.presentation.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

public class AdminBannerRequest {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotBlank(message = "배너 제목은 필수입니다.")
        private String title;

        @NotNull(message = "배너 이미지는 필수입니다.")
        private MultipartFile image;

        private String linkUrl;

        private String description;

        @NotNull(message = "노출 순서는 필수입니다.")
        private Integer displayOrder;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        @NotBlank(message = "배너 제목은 필수입니다.")
        private String title;

        private MultipartFile image; // 이미지 변경 시에만 전송

        private String linkUrl;

        private String description;

        @NotNull(message = "노출 순서는 필수입니다.")
        private Integer displayOrder;
    }
}

