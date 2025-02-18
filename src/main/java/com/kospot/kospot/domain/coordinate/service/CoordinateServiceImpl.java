package com.kospot.kospot.domain.coordinate.service;

import com.kospot.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinateIdCache.adaptor.CoordinateIdCacheAdaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CoordinateServiceImpl implements CoordinateService {

    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateIdCacheAdaptor coordinateIdCacheAdaptor;

    @Override
    public Coordinate getRandomCoordinateBySido(String sidoKey) {
        Sido sido = Sido.fromKey(sidoKey);
        Long maxId = getMaxId(sido);
        Long randomIndex = getRandomIndex(maxId);

        while (!coordinateAdaptor.queryExistsById(sido, randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryById(sido, randomIndex);
    }

    @Override
    public Coordinate getRandomNationwideCoordinate() {
        Long maxId = getMaxId(Sido.NATIONWIDE);
        Long randomIndex = getRandomIndex(maxId);

        while (!coordinateAdaptor.queryNationwideExistsById(randomIndex)) {
            randomIndex++;
        }

        return coordinateAdaptor.queryNationwideById(randomIndex);
    }

    private Long getMaxId(Sido sido) {
        return coordinateIdCacheAdaptor.queryById(sido).getMaxId();
    }

    private Long getRandomIndex(Long maxId) {
        return ThreadLocalRandom.current().nextLong(1, maxId + 1);
    }


}
