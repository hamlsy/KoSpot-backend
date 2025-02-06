package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;
import com.kospot.kospot.domain.coordinateIdCache.repository.CoordinateIdCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CoordinateServiceImpl implements CoordinateService{

    private final CoordinateRepository coordinateRepository;
    private final CoordinateIdCacheRepository coordinateIdCacheRepository;
    private final DynamicCoordinateRepositoryFactory factory;

    //todo refactoring needed
    @Override
    public Location getRandomCoordinateBySido(String sidoName) {
        Sido sido = Sido.fromName(sidoName);
        Long maxId = coordinateIdCacheRepository.findById(sido).orElseThrow(
                () -> new IllegalArgumentException("캐시 테이블의 ID 값이 존재하지 않습니다.")
        ).getMaxId();

        Long randomIndex = ThreadLocalRandom.current().nextLong(maxId);
        BaseCoordinateRepository<?, Long> repository = factory.getRepository(sido);

        do {
            //todo refactor exception
            if (repository.existsById(randomIndex)) {
                break;
            }
            randomIndex++;
        }while(true);

        return (Location) repository.findById(randomIndex).orElseThrow(
                () -> new IllegalArgumentException("해당 시도의 좌표가 존재하지 않습니다.")
        );
    }

    //todo refactoring needed
//    private List<Coordinate> findCoordinatesBySido(String sidoString) {
//        Sido sido = Sido.fromName(sidoString);
//        return coordinateRepository.findByAddress_Sido(sido).orElseThrow(
//                () -> new IllegalArgumentException("해당 시도의 좌표가 존재하지 않습니다.")
//        );
//    }

    //todo refactoring needed
    @Override
    public Coordinate getRandomCoordinate(){
        List<Coordinate> coordinates = coordinateRepository.findAll();
        int randomIndex = ThreadLocalRandom.current().nextInt(coordinates.size());
        return coordinates.get(randomIndex);
    }


}
