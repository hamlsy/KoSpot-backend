package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Location;
import jakarta.persistence.Entity;

@Entity
public class CoordinateJeonbuk extends Location {
    public CoordinateJeonbuk(Coordinate coordinate){
        super(coordinate);
    }
}
