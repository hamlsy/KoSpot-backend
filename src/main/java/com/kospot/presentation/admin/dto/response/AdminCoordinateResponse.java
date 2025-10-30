package com.kospot.presentation.admin.dto.response;

import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.LocationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class AdminCoordinateResponse {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CoordinateInfo {
        private Long coordinateId;
        private Double lat;
        private Double lng;
        private String poiName;
        private String sido;
        private String sigungu;
        private String detailAddress;
        private LocationType locationType;

        public static CoordinateInfo from(Coordinate coordinate) {
            return CoordinateInfo.builder()
                    .coordinateId(coordinate.getId())
                    .lat(coordinate.getLat())
                    .lng(coordinate.getLng())
                    .poiName(coordinate.getPoiName())
                    .sido(coordinate.getAddress().getSido().getName())
                    .sigungu(coordinate.getAddress().getSigungu())
                    .detailAddress(coordinate.getAddress().getDetailAddress())
                    .locationType(coordinate.getLocationType())
                    .build();
        }
    }
}

