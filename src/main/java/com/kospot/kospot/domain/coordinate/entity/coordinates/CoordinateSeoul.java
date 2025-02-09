package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateSeoul extends Coordinate {
    public CoordinateSeoul(CoordinateNationwide coordinate) {
        super(coordinate);
    }
}
