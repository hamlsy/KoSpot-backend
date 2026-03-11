package com.kospot.coordinate.presentation.response;

import com.kospot.coordinate.domain.vo.Address;
import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.coordinate.domain.entity.LocationType;
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
