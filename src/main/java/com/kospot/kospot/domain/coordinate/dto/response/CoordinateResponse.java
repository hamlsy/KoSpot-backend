package com.kospot.kospot.domain.coordinate.dto.response;

import com.kospot.kospot.domain.coordinate.entity.Address;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CoordinateResponse {
    private Address address;
    private double lat;
    private double lng;
    private LocalDateTime createdDate;

    public static CoordinateResponse from(Coordinate coordinate) {
        return CoordinateResponse.builder()
                .address(coordinate.getAddress())
                .lat(coordinate.getLat())
                .lat(coordinate.getLng())
                .createdDate(coordinate.getCreatedDate())
                .build();
    }
}
