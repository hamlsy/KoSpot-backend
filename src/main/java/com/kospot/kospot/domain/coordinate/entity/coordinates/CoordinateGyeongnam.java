package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Location;
import jakarta.persistence.Entity;

@Entity
public class CoordinateGyeongnam extends Location {
    public CoordinateGyeongnam(Coordinate coordinate){
        super(coordinate);
    }
}
