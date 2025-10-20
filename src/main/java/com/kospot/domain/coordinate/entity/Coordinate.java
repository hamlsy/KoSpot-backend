package com.kospot.domain.coordinate.entity;

import com.kospot.domain.auditing.entity.BaseTimeEntity;
import com.kospot.domain.coordinate.vo.Address;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "coordinates",
indexes = {
        @Index(name = "idx_coordinate_sido", columnList = "sido")
})
public class Coordinate extends BaseTimeEntity {
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

}
