package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateGwangju extends Coordinate {
    public CoordinateGwangju(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
