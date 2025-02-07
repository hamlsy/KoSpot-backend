package com.kospot.kospot.domain.coordinate.dto.response;

import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RandomCoordinateResponse {

    private double lat;
    private double lng;

    public static RandomCoordinateResponse from(Location coordinate){
        return RandomCoordinateResponse.builder()
                .lat(coordinate.getLat())
                .lng(coordinate.getLng())
                .build();
    }

}
