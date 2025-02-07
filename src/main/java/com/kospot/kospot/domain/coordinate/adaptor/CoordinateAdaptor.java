package com.kospot.kospot.domain.coordinate.adaptor;

import com.kospot.kospot.domain.coordinate.entity.Location;

public interface CoordinateAdaptor {
    Location queryById(Long id);

    boolean queryExistsById(Long id);
}
