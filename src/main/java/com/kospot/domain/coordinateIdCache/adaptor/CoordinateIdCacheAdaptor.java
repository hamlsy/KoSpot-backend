package com.kospot.domain.coordinateIdCache.adaptor;

import com.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;
import com.kospot.domain.coordinateIdCache.repository.CoordinateIdCacheRepository;
import com.kospot.exception.object.domain.CoordinateIdCacheHandler;
import com.kospot.exception.payload.code.ErrorStatus;

import com.kospot.global.annotation.adaptor.Adaptor;
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
