package com.kospot.domain.coordinateidcache.service;

import com.kospot.domain.coordinate.adaptor.CoordinateAdaptor;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinateidcache.entity.CoordinateIdCache;
import com.kospot.domain.coordinateidcache.repository.CoordinateIdCacheRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CoordinateIdCacheService {

    private final CoordinateAdaptor coordinateAdaptor;
    private final CoordinateIdCacheRepository repository;

    public void saveAllMaxId() {
        for (Sido sido : Sido.values()) {
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
