package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
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

    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateIdCacheRepository coordinateIdCacheRepository;
    private final DynamicCoordinateRepositoryFactory factory;

    @Override
    public Location getRandomCoordinateBySido(String sidoKey) {
        Sido sido = Sido.fromKey(sidoKey);
        Long maxId = getMaxId(sido);

        Long randomIndex = ThreadLocalRandom.current().nextLong(maxId);
        BaseCoordinateRepository<?, Long> repository = factory.getRepository(sido);

        while (!repository.existsById(randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryById(randomIndex);
    }

    @Override
    public Location getAllRandomCoordinate(){
        Long maxId = getMaxId(Sido.NATIONWIDE);
        Long randomIndex = ThreadLocalRandom.current().nextLong(maxId);

        while (!coordinateAdaptor.queryExistsById(randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryById(randomIndex);

    }

    private Long getMaxId(Sido sido){
        return coordinateIdCacheRepository.findById(sido).orElseThrow(
                () -> new CoordinateHandler(ErrorStatus.COORDINATE_CACHE_TABLE_ID_NOT_FOUND)
        ).getMaxId();
    }

}
