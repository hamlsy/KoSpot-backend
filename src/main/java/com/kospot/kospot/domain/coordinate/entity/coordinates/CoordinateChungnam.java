package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Location;
import jakarta.persistence.Entity;

@Entity
public class CoordinateChungnam extends Location {
    public CoordinateChungnam(Coordinate coordinate){
        super(coordinate);
    }
}
