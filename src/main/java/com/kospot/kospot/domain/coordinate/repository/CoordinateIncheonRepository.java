package com.kospot.kospot.domain.coordinate.repository;

import com.kospot.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.kospot.domain.coordinate.entity.coordinates.CoordinateIncheon;
import com.kospot.kospot.domain.coordinate.entity.sido.Sido;

@SidoRepository(Sido.INCHEON)
public interface CoordinateIncheonRepository extends CoordinateRepository<CoordinateIncheon, Long> {

}
