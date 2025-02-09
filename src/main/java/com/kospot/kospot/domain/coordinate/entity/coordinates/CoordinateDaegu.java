package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateDaegu extends Coordinate {
    public CoordinateDaegu(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
