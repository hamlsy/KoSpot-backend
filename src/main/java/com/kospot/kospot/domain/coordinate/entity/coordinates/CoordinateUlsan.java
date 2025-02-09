package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateUlsan extends Coordinate {
    public CoordinateUlsan(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
