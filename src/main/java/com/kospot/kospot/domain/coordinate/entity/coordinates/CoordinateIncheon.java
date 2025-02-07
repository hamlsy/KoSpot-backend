package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Location;
import jakarta.persistence.Entity;

@Entity
public class CoordinateIncheon extends Location {
    public CoordinateIncheon(Coordinate coordinate){
        super(coordinate);
    }
}
