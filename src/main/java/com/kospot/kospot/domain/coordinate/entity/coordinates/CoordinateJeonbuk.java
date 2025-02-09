package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateJeonbuk extends Coordinate {
    public CoordinateJeonbuk(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
