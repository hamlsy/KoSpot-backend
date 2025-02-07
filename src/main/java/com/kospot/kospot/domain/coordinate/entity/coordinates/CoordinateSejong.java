package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Location;
import jakarta.persistence.Entity;

@Entity
public class CoordinateSejong extends Location {
    public CoordinateSejong(Coordinate coordinate) {
        super(coordinate);
    }
}
