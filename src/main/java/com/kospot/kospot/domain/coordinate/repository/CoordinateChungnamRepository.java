package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateChungnam;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.CHUNGNAM)
public interface CoordinateChungnamRepository extends CoordinateRepository<CoordinateChungnam, Long> {

}
