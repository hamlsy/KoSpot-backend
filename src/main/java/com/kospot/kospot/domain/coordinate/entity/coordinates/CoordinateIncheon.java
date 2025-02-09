package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateIncheon extends Coordinate {
    public CoordinateIncheon(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
