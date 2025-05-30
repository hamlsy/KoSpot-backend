package com.kospot.domain.coordinate.entity.coordinates;

import com.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoordinateGyeongnam extends Coordinate {
    public CoordinateGyeongnam(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
