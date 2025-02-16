package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateDaejeon;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.DAEJEON)
public interface CoordinateDaejeonRepository extends CoordinateRepository<CoordinateDaejeon, Long> {

}
