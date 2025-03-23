package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateGangwon;
import com.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.GANGWON)
public interface CoordinateGangwonRepository extends CoordinateRepository<CoordinateGangwon, Long> {

}
