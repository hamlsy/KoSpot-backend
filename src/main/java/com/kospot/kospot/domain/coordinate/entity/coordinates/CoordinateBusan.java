package com.kospot.kospot.domain.coordinate.entity.coordinates;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.*;

@Entity
public class CoordinateBusan extends Coordinate {
    public CoordinateBusan(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
