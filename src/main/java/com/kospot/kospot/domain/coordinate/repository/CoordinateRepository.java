package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.Coordinate;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.NATIONWIDE)
public interface CoordinateRepository extends BaseCoordinateRepository<Coordinate, Long> {

}
