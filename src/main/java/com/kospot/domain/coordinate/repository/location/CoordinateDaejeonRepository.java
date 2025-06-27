package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateDaejeon;
import com.kospot.domain.coordinate.vo.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.DAEJEON)
public interface CoordinateDaejeonRepository extends CoordinateRepository<CoordinateDaejeon, Long> {

}
