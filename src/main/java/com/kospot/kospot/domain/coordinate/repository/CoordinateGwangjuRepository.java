package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateGwangju;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.GWANGJU)
public interface CoordinateGwangjuRepository extends CoordinateRepository<CoordinateGwangju, Long> {

}
