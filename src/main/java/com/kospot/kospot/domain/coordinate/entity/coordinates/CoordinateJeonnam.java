package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateJeonnam extends Coordinate {
    public CoordinateJeonnam(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
