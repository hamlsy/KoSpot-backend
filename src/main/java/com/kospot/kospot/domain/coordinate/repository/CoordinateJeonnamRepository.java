package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateJeonnam;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.JEONNAM)
public interface CoordinateJeonnamRepository extends BaseCoordinateRepository<CoordinateJeonnam, Long> {

}
