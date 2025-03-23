package com.kospot.domain.coordinate.entity.coordinates;

import com.kospot.domain.coordinate.entity.Coordinate;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoordinateGangwon extends Coordinate {
    public CoordinateGangwon(CoordinateNationwide coordinate){
        super(coordinate);
    }
}
