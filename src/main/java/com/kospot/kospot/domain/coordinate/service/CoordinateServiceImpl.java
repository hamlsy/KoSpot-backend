package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.entity.Location;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.domain.coordinate.repository.CoordinateRepository;
import com.kospot.kospot.domain.coordinateIdCache.repository.CoordinateIdCacheRepository;
import com.kospot.kospot.exception.object.domain.CoordinateHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CoordinateServiceImpl implements CoordinateService{

    private final CoordinateRepository coordinateRepository;
    private final CoordinateIdCacheRepository coordinateIdCacheRepository;
    private final DynamicCoordinateRepositoryFactory factory;

    @Override
    public Location getRandomCoordinateBySido(String sidoKey) {
        Sido sido = Sido.fromKey(sidoKey);
        Long maxId = getMaxId(sido);

        Long randomIndex = ThreadLocalRandom.current().nextLong(maxId);
        BaseCoordinateRepository<?, Long> repository = factory.getRepository(sido);

        do {
            if (repository.existsById(randomIndex)) {
                break;
            }
            randomIndex++;
        }while(true);

        return (Location) repository.findById(randomIndex).orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
        );
    }

    @Override
    public Coordinate getAllRandomCoordinate(){
        Long maxId = getMaxId(Sido.NATIONWIDE);
        Long randomIndex = ThreadLocalRandom.current().nextLong(maxId);

        do {
            if (coordinateRepository.existsById(randomIndex)) {
                break;
            }
            randomIndex++;
        }while(true);

        return coordinateRepository.findById(randomIndex).orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_NOT_FOUND)
        );

    }

    private Long getMaxId(Sido sido){
        return coordinateIdCacheRepository.findById(sido).orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_CACHE_TABLE_ID_NOT_FOUND)
        ).getMaxId();
    }

}
