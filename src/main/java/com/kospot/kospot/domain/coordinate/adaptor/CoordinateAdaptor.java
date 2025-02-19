package com.kospot.kospot.domain.coordinate.adaptor;

import com.kospot.kospot.domain.coordinate.entity.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

public interface CoordinateAdaptor {

    Coordinate queryById(Sido sido, Long id);

    boolean queryExistsById(Sido sido, Long id);

    Long queryMaxIdBySido(Sido sido);

    Coordinate queryNationwideById(Long id);

    Long queryNationwideMaxId();

    boolean queryNationwideExistsById(Long id);
}
