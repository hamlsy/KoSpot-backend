package com.kospot.coordinate.application.service;

import com.kospot.coordinate.domain.entity.Coordinate;
import com.kospot.coordinate.infrastructure.persistence.CoordinateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CoordinateService {

    private final CoordinateRepository coordinateRepository;

    public void invalidateCoordinate(Coordinate coordinate) {
        coordinate.invalidate();
    }

    public Coordinate createCoordinate(Coordinate coordinate) {
        return coordinateRepository.save(coordinate);
    }

    public void deleteCoordinate(Coordinate coordinate) {
        coordinateRepository.delete(coordinate);
    }

    public void saveAllCoordinates(List<Coordinate> coordinates) {
        coordinateRepository.saveAll(coordinates);
    }
}

