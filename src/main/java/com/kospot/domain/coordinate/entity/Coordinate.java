package com.kospot.domain.coordinate.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.domain.coordinate.vo.LocationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class Coordinate extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double lat;

    private double lng;

    private String poiName;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    private LocationType locationType;

    public Coordinate(CoordinateNationwide coordinate) {
        this.address = coordinate.getAddress();
        this.lng = coordinate.getLng();
        this.lat = coordinate.getLat();
        this.poiName = coordinate.getPoiName();
        this.locationType = coordinate.getLocationType();
    }

}
