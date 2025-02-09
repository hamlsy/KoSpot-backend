package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateGyeongbuk extends Coordinate {
    public CoordinateGyeongbuk(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
