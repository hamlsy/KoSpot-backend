package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Location;
import jakarta.persistence.Entity;

@Entity
public class CoordinateDaejeon extends Location {
    public CoordinateDaejeon(Coordinate coordinate){
        super(coordinate);
    }
}
