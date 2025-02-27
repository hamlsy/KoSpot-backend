package com.kospot.kospot.domain.coordinate.repository.location;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateDaejeon;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.DAEJEON)
public interface CoordinateDaejeonRepository extends CoordinateRepository<CoordinateDaejeon, Long> {

}
