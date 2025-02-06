package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateBusan;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.BUSAN)
public interface CoordinateBusanRepository extends BaseCoordinateRepository<CoordinateBusan, Long> {

}
