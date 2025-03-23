package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateDaegu;
import com.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.DAEGU)
public interface CoordinateDaeguRepository extends CoordinateRepository<CoordinateDaegu, Long> {

}
