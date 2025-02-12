package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateJeju;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.JEJU)
public interface CoordinateJejuRepository extends CoordinateRepository<CoordinateJeju, Long> {

}
