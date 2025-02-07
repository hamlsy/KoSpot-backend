package com.kospot.kospot.domain.coordinate.service;


import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;

public interface CoordinateService {
    Location getRandomCoordinateBySido(String sido);

    Location getAllRandomCoordinate();
}
