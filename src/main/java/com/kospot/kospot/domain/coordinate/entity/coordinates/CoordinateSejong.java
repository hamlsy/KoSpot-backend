package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateSejong extends Coordinate {
    public CoordinateSejong(CoordinateNationwide coordinate) {
        super(coordinate);
    }
}
