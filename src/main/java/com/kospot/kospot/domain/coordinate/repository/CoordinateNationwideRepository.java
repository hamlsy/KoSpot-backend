package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateNationwide;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.NATIONWIDE)
public interface CoordinateNationwideRepository extends BaseCoordinateRepository<CoordinateNationwide, Long> {

}
