package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Location;
import jakarta.persistence.Entity;

@Entity
public class CoordinateSeoul extends Location {
    public CoordinateSeoul(Coordinate coordinate) {
        super(coordinate);
    }
}
