package com.kospot.presentation.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class AdminCoordinateRequest {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        @NotNull(message = "위도는 필수입니다.")
        private Double lat;

        @NotNull(message = "경도는 필수입니다.")
        private Double lng;

        @NotBlank(message = "POI 이름은 필수입니다.")
        private String poiName;

        @NotBlank(message = "시도는 필수입니다.")
        private String sidoKey;

        @NotBlank(message = "시군구는 필수입니다.")
        private String sigungu;

        @NotBlank(message = "상세 주소는 필수입니다.")
        private String detailAddress;

        @NotBlank(message = "위치 타입은 필수입니다.")
        private String locationType;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportExcel {
        @NotBlank(message = "파일 이름은 필수입니다.")
        private String fileName;
    }
}

