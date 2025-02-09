package com.kospot.kospot.domain.coordinate.adaptor;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;

public interface CoordinateAdaptor {
    Coordinate queryById(Long id);

    boolean queryExistsById(Long id);
}
