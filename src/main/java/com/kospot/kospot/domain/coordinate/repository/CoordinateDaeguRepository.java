package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateDaegu;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.DAEGU)
public interface CoordinateDaeguRepository extends CoordinateRepository<CoordinateDaegu, Long> {

}
