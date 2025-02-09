package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;

@Entity
public class CoordinateGangwon extends Coordinate {
    public CoordinateGangwon(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
