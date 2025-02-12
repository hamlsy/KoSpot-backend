package com.kospot.kospot.domain.coordinateIdCache.service;

import com.kospot.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.service.DynamicCoordinateRepositoryFactory;
import com.kospot.kospot.domain.coordinateIdCache.adaptor.CoordinateIdCacheAdaptor;
import com.kospot.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;
import com.kospot.kospot.domain.coordinateIdCache.repository.CoordinateIdCacheRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CoordinateIdCacheServiceImpl implements CoordinateIdCacheService {

    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateIdCacheRepository repository;

    @Override
    @Transactional
    public void saveAllMaxId(){
        for(Sido sido : Sido.values()){
            saveMaxIdBySido(sido);
        }
    }

    public void saveMaxIdBySido(Sido sido) {
        Long maxId = coordinateAdaptor.queryMaxIdBySido(sido);

        repository.findById(sido).ifPresentOrElse(
                coordinateIdCache -> coordinateIdCache.updateMaxId(maxId),
                () -> repository.save(CoordinateIdCache.builder()
                        .sido(sido)
                        .maxId(maxId)
                        .build())
        );
    }

}
