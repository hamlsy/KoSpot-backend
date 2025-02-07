package com.kospot.kospot.domain.coordinateIdCache.adaptor;

import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinateIdCache.entity.CoordinateIdCache;

public interface CoordinateIdCacheAdaptor {
    CoordinateIdCache queryById(Sido sido);
}
