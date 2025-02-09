package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateJeju extends Coordinate {
    public CoordinateJeju(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
