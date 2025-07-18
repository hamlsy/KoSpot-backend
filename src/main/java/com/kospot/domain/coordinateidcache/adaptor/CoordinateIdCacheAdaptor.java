package com.kospot.domain.coordinateidcache.adaptor;

import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinateidcache.entity.CoordinateIdCache;
import com.kospot.domain.coordinateidcache.repository.CoordinateIdCacheRepository;
import com.kospot.infrastructure.exception.object.domain.CoordinateIdCacheHandler;
import com.kospot.infrastructure.exception.payload.code.ErrorStatus;

import com.kospot.infrastructure.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CoordinateIdCacheAdaptor {

    private final CoordinateIdCacheRepository repository;

    public CoordinateIdCache queryById(Sido sido) {
        return repository.findById(sido).orElseThrow(
                () -> new CoordinateIdCacheHandler(ErrorStatus.COORDINATE_CACHE_TABLE_ID_NOT_FOUND)
        );
    }
}
