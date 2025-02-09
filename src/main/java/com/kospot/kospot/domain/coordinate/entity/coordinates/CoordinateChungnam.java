package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateChungnam extends Coordinate {
    public CoordinateChungnam(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
