package com.kospot.domain.coordinate.entity.coordinates;

import com.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoordinateGyeongbuk extends Coordinate {
    public CoordinateGyeongbuk(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
