package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateGyeongnam extends Coordinate {
    public CoordinateGyeongnam(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
