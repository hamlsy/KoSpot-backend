package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateGyeongnam;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.GYEONGNAM)
public interface CoordinateGyeongnamRepository extends BaseCoordinateRepository<CoordinateGyeongnam, Long> {

}
