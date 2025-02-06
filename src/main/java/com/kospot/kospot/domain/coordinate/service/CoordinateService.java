package com.kospot.kospot.domain.coordinate.service;


import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;

public interface CoordinateService {
    Coordinate getRandomCoordinateBySido(String sido);

    Coordinate getRandomCoordinate();
}
