package com.kospot.kospot.domain.coordinate.dto.response;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RandomCoordinateResponse {

    private double lat;
    private double lng;

    public static RandomCoordinateResponse from(Coordinate coordinate){
        return RandomCoordinateResponse.builder()
                .lat(coordinate.getLat())
                .lng(coordinate.getLng())
                .build();
    }

}
