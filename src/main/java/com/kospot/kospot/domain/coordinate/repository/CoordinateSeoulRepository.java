package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateSeoul;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.SEOUL)
public interface CoordinateSeoulRepository extends BaseCoordinateRepository<CoordinateSeoul, Long> {

}
