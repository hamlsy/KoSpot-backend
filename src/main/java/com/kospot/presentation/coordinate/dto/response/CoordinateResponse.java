package com.kospot.presentation.coordinate.dto.response;

import com.kospot.domain.coordinate.vo.Address;
import com.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.domain.coordinate.entity.LocationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CoordinateResponse {
    private Address address;
    private double lat;
    private double lng;
    private LocationType locationType;
    private LocalDateTime createdDate;
    private String poiName;

    public static CoordinateResponse from(Coordinate coordinate) {
        return CoordinateResponse.builder()
                .address(coordinate.getAddress())
                .lat(coordinate.getLat())
                .lng(coordinate.getLng())
                .locationType(coordinate.getLocationType())
                .createdDate(coordinate.getCreatedDate())
                .poiName(coordinate.getPoiName())
                .build();
    }
}
