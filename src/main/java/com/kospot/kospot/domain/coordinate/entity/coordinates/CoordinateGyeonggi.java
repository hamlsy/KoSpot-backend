package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateGyeonggi extends Coordinate {
    public CoordinateGyeonggi(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
