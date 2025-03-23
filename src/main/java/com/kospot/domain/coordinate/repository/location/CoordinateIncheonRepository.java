package com.kospot.domain.coordinate.repository.location;

import com.kospot.domain.coordinate.aop.SidoRepository;
import com.kospot.domain.coordinate.entity.coordinates.CoordinateIncheon;
import com.kospot.domain.coordinate.entity.sido.Sido;
import com.kospot.domain.coordinate.repository.base.CoordinateRepository;

@SidoRepository(Sido.INCHEON)
public interface CoordinateIncheonRepository extends CoordinateRepository<CoordinateIncheon, Long> {

}
