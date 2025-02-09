package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateDaejeon extends Coordinate {
    public CoordinateDaejeon(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
