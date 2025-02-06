package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.region.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CoordinateServiceImpl implements CoordinateService{

    private final CoordinateRepository coordinateRepository;

    @Override
    public Coordinate getRandomCoordinateBySido(String sido) {
        List<Coordinate> coordinates = findCoordinatesBySido(sido);
        int randomIndex = ThreadLocalRandom.current().nextInt(coordinates.size());
        return coordinates.get(randomIndex);
    }

    private List<Coordinate> findCoordinatesBySido(String sidoString) {
        Sido sido = Sido.fromName(sidoString);
        return coordinateRepository.findByAddress_Sido(sido).orElseThrow(
                () -> new IllegalArgumentException("해당 시도의 좌표가 존재하지 않습니다.")
        );
    }

    //todo refactoring needed
    @Override
    public Coordinate getRandomCoordinate(){
        List<Coordinate> coordinates = coordinateRepository.findAll();
        int randomIndex = ThreadLocalRandom.current().nextInt(coordinates.size());
        return coordinates.get(randomIndex);
    }


}
