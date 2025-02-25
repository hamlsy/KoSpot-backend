package com.kospot.kospot.domain.coordinateIdCache.adaptor;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;
import com.kospot.kospot.domain.coordinateIdCache.repository.CoordinateIdCacheRepository;
import com.kospot.kospot.exception.object.domain.CoordinateIdCacheHandler;
import com.kospot.kospot.exception.payload.code.ErrorStatus;

import com.kospot.kospot.global.annotation.adaptor.Adaptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
