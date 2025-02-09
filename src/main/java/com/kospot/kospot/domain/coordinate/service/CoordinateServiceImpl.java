package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.BaseCoordinateRepository;
import com.kospot.kospot.domain.coordinateIdCache.adaptor.CoordinateIdCacheAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CoordinateServiceImpl implements CoordinateService {

    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateIdCacheAdaptor coordinateIdCacheAdaptor;
    private final DynamicCoordinateRepositoryFactory factory;

    @Override
    public Coordinate getRandomCoordinateBySido(String sidoKey) {
        Sido sido = Sido.fromKey(sidoKey);
        Long maxId = getMaxId(sido);
        Long randomIndex = getRandomIndex(maxId);

        BaseCoordinateRepository<?, Long> repository = factory.getRepository(sido);

        while (!repository.existsById(randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryById(randomIndex);
    }

    @Override
    public Coordinate getAllRandomCoordinate() {
        Long maxId = getMaxId(Sido.NATIONWIDE);
        Long randomIndex = getRandomIndex(maxId);

        while (!coordinateAdaptor.queryExistsById(randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryById(randomIndex);
    }

    private Long getMaxId(Sido sido) {
        return coordinateIdCacheAdaptor.queryById(sido).getMaxId();
    }

    private Long getRandomIndex(Long maxId) {
        return ThreadLocalRandom.current().nextLong(maxId);
    }


}
